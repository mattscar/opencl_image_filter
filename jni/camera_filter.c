#include <CL/cl.h>
#include "camera_filter.h"
#include <stdio.h>
#include <string.h>

JNIEXPORT jint JNICALL Java_com_ee_camerafilter_CameraFilterView_cameraFilter
	(JNIEnv *env, jobject obj, jint width, jint height, jobject input_buff,
     jobject output_buff, jobject prog_buff) {

	cl_platform_id platform;
	cl_device_id device;
	cl_context context;
	cl_program program;
	cl_mem input_mem, output_mem, filter_mem;
	cl_command_queue queue;
	cl_kernel kernel;
	size_t global_size;
	int program_size, int_width;
	jint err = 0;

	void *input_ptr = (*env)->GetDirectBufferAddress(env, input_buff);
	void *output_ptr = (*env)->GetDirectBufferAddress(env, output_buff);
	char *program_buffer = (char*)((*env)->GetDirectBufferAddress(env, prog_buff));

	/* Identify a platform */
	err = clGetPlatformIDs(1, &platform, NULL);
	if(err < 0) {
		return err;
	}

	/* Access the first device */
	err = clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 1, &device, NULL);
	if(err < 0) {
		return err;
	}

	/* Create a context */
	context = clCreateContext(NULL, 1, &device, NULL, NULL, &err);
	if(err < 0) {
		return err;
	}

	/* Create the program */
	program_size = strlen(program_buffer);
	program = clCreateProgramWithSource(context, 1,
			(const char**)&program_buffer, &program_size, &err);
	if(err < 0) {
		return err;
	}

	/* Build the program */
	err = clBuildProgram(program, 0, NULL, NULL, NULL, NULL);
	if(err < 0) {
		return err;
	}

	kernel = clCreateKernel(program, "camera_filter", &err);
	if(err < 0) {
		return err;
	};

	/* Create the buffer containing input data */
	input_mem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
	                           width * height * sizeof(int), input_ptr, &err);
	if(err < 0) {
		return err;
	};

	/* Create the buffer containing input data */
	output_mem = clCreateBuffer(context, CL_MEM_WRITE_ONLY, width * height * sizeof(int),
			                    NULL, &err);
	if(err < 0) {
		return err;
	};

	/* Create kernel arguments */
	err = clSetKernelArg(kernel, 0, sizeof(cl_mem), &input_mem);
	err |= clSetKernelArg(kernel, 1, sizeof(cl_mem), &output_mem);
	err |= clSetKernelArg(kernel, 2, sizeof(width), &width);
	err |= clSetKernelArg(kernel, 3, sizeof(width), &height);
	if(err < 0) {
		return err;
	};

	/* Create a command queue */
	queue = clCreateCommandQueue(context, device, 0, &err);
	if(err < 0) {
		return err;
	};

	/* Execute the kernel */
	global_size = width * height;
	err = clEnqueueNDRangeKernel(queue, kernel, 1, NULL, &global_size, NULL, 0, NULL, NULL);
	if(err < 0) {
		return err;
	}

	/* Read the result */
	err = clEnqueueReadBuffer(queue, output_mem, CL_TRUE, 0, width * height * sizeof(int),
	                          output_ptr, 0, NULL, NULL);
	if(err < 0) {
		return err;
	}

	/* Deallocate resources */
	clReleaseMemObject(output_mem);
	clReleaseMemObject(filter_mem);
	clReleaseMemObject(input_mem);
	clReleaseKernel(kernel);
	clReleaseCommandQueue(queue);
	clReleaseProgram(program);
	clReleaseContext(context);
	return 0;
}
