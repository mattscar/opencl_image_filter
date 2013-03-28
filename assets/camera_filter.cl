__kernel void camera_filter(__global int *input_img, __global int *out_img, int width, int height) {

   int offset, red, green, blue;
   int row = get_global_id(0)/width;
   int col = get_global_id(0) - row * width;
   int4 pixels[3], color_vec;

   /* Set filter coefficients */ 
   int4 k0 = (int4)(-1, -1, 0, 0);
   int4 k1 = (int4)(-1, 0, 1, 0);
   int4 k2 = (int4)(0, 1, 1, 0);   

   /* Set filter denominator */
   int denom = 1;
   
   if((row > 0) && (col > 0) && (col < width-2) && (row < height-1)) {
   
      /* Read input image data into private memory */
      offset = (row-1) * width + (col-1);
      pixels[0] = vload4(0, input_img + offset);
      pixels[1] = vload4(0, input_img + offset + width);
      pixels[2] = vload4(0, input_img + offset + 2*width);

      /* Compute red component */
      color_vec = ((pixels[0] & 0x00ff0000) >> 16) * k0 + 
                  ((pixels[1] & 0x00ff0000) >> 16) * k1 +
                  ((pixels[2] & 0x00ff0000) >> 16) * k2;
      red = clamp((color_vec.s0 + color_vec.s1 + color_vec.s2)/denom, 0, 255);

      /* Compute green component */
      color_vec = ((pixels[0] & 0x0000ff00) >> 8) * k0 + 
                  ((pixels[1] & 0x0000ff00) >> 8) * k1 +
                  ((pixels[2] & 0x0000ff00) >> 8) * k2;
      green = clamp((color_vec.s0 + color_vec.s1 + color_vec.s2)/denom, 0, 255);

      /* Compute blue component */
      color_vec = (pixels[0] & 0x000000ff) * k0 + 
                  (pixels[1] & 0x000000ff) * k1 +
                  (pixels[2] & 0x000000ff) * k2;
      blue = clamp((color_vec.s0 + color_vec.s1 + color_vec.s2)/denom, 0, 255);

      /* Update output pixel in global memory */
      out_img[get_global_id(0)] = 0xff000000 + (red << 16) + (green << 8) + blue;
   }
   else {
      out_img[get_global_id(0)] = input_img[get_global_id(0)];
   }
}
