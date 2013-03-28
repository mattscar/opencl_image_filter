package com.ee.camerafilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class CameraFilterView extends SurfaceView 
             implements SurfaceHolder.Callback, Handler.Callback {

  private Camera cam;
  ImageView outView;
  int pictureWidth, pictureHeight;
  ByteBuffer inputBuffer, outputBuffer, progBuffer;
  Camera.PictureCallback callBack;
  byte[] pictureData;
  Bitmap output;
  GpuThread gpuThread;
  Handler mainHandler;
  boolean cameraOpen = false;
  
  /*
  ByteBuffer filterBuffer;
  int[] filterValues = new int[]{0, 0, 0, 0,
                                 0, 1, 0, 0,
                                 0, 0, 0, 1,
                                 0, 0, 0, 0};
  */
  static boolean sfoundLibrary = true;
  
  static {
    try {
      System.load("/system/vendor/lib/egl/libGLES_mali.so");
      System.loadLibrary("CameraFilter");  
    }
    catch (UnsatisfiedLinkError e) {
      sfoundLibrary = false;
    }
  }

  /** Calls OpenCL commands to test the GPU */
  private native int cameraFilter(int w, int h, ByteBuffer input, ByteBuffer output, ByteBuffer prog);  
  
  class GpuThread extends Thread {
    
    public void run() {
      output = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
      output.copyPixelsToBuffer(inputBuffer);
      inputBuffer.rewind();
      int err = cameraFilter(pictureWidth, pictureHeight, inputBuffer, 
                             outputBuffer, progBuffer);
      output.copyPixelsFromBuffer(outputBuffer);
      outputBuffer.rewind();

      // Create and send message
      Message msg = new Message();
      msg.what = (int)err;
      mainHandler.sendMessage(msg);
    }
  }
  
  public CameraFilterView(Context context) {
    super(context);
    init();
  }
  
  public CameraFilterView(Context context, AttributeSet attrs) {
    super(context);
    init();
  }  
  
  private void init() {
    getHolder().addCallback(this);
    progBuffer = getProgramBuffer();
    gpuThread = new GpuThread();
    mainHandler = new Handler(this);
    
    /*
    // Create the filter buffer to hold twelve int values
    filterBuffer = ByteBuffer.allocateDirect(16 * 4);
    filterBuffer.asIntBuffer().put(filterValues);
    filterBuffer.rewind();
    */
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {

    callBack = new Camera.PictureCallback() {
      public void onPictureTaken(byte[] data, Camera camera) {
        pictureData = data;
        if(sfoundLibrary)
          gpuThread.start();
        else
          Toast.makeText(getContext(), "OpenCL library could not be accessed", Toast.LENGTH_SHORT).show();
        cam.startPreview();
      }
    };

    // Access other view
    outView = (ImageView)(((CameraFilterActivity)getContext()).findViewById(R.id.image_view));
    
    // Determine size of preview images
    cam = Camera.open();
    cameraOpen = true;

    // Take picture button
    Button pictureButton = (Button)(((CameraFilterActivity)getContext()).
        findViewById(R.id.snap_button));
    pictureButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View v) {
        cam.takePicture(null, null, null, callBack);
      }
    });
    
    /*
    // Set filter button
    Button filterButton = (Button)(((CameraFilterActivity)getContext()).
        findViewById(R.id.filter_button));
    filterButton.setOnClickListener(new View.OnClickListener() {
      
      @Override
      public void onClick(View v) {
        final CameraFilterDialog dialog = new CameraFilterDialog(v.getContext(), filterValues);
        dialog.show();
      }
    });
    */
    
    // Close application button
    Button closeButton = (Button)(((CameraFilterActivity)getContext()).
        findViewById(R.id.close_button));
    closeButton.setOnClickListener(new View.OnClickListener() {

      public void onClick(View v) {
        if(cameraOpen) {
          cam.stopPreview();
          cam.unlock();
          cam.release();
          cameraOpen = false;
        }
        ((CameraFilterActivity)v.getContext()).finish();
      }
    });
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    
    Camera.Parameters p = cam.getParameters();
    for(Camera.Size s : p.getSupportedPictureSizes()) {
      if(s.width <= width && s.height <= height) {
        pictureWidth = s.width; pictureHeight = s.height;
        p.setPictureSize(s.width, s.height);
        inputBuffer = ByteBuffer.allocateDirect(s.width * s.height * 4);
        outputBuffer = ByteBuffer.allocateDirect(s.width * s.height * 4);
        break;
      }
    }
    for(Camera.Size s : p.getSupportedPreviewSizes()) {
      if(s.width <= pictureWidth && s.height <= pictureHeight) {
        p.setPreviewSize(s.width, s.height);
        break;
      }
    }
    p.setPictureFormat(ImageFormat.JPEG);
    cam.setParameters(p);

    try {
      cam.setPreviewDisplay(getHolder());
    }
    catch(IOException e) {
      e.printStackTrace();
    }

    cam.startPreview();
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    if(cameraOpen) {
      cam.stopPreview();
      cam.unlock();
      cam.release();
      cameraOpen = false;
    }
  }

  // Read kernel file
  private ByteBuffer getProgramBuffer() {

    String line;
    InputStream stream;
    byte[] programBytes;
    StringBuilder program = new StringBuilder();
    ByteBuffer buffer = null;

    try {
      stream = getContext().getResources().getAssets().open("camera_filter.cl");
      BufferedReader br = new BufferedReader(new InputStreamReader(stream)); 
      while((line = br.readLine()) != null) { 
        program.append(line); 
      }
      stream.close(); 
      br.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    program.append("\0");
    
    // Create ByteBuffer
    try {
      programBytes = program.toString().getBytes("UTF-8");
      buffer = ByteBuffer.allocateDirect(programBytes.length);
      buffer.put(programBytes);
      buffer.rewind();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    return buffer;
  }

  @Override
  public boolean handleMessage(Message msg) {
    int err = msg.what;
    if(err < 0)
      Toast.makeText(getContext(), "Error: " + err, Toast.LENGTH_SHORT).show();
    outView.setImageBitmap(output);
    return true;
  }
}
