package com.ee.camerafilter;

import android.os.Bundle;
import android.app.Activity;

public class CameraFilterActivity extends Activity {

  public void onCreate(Bundle b) {
    super.onCreate(b);
    setTheme(android.R.style.Theme_Holo);
    setContentView(R.layout.camera_test);
  }
}