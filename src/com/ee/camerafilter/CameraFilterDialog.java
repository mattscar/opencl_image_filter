package com.ee.camerafilter;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

public class CameraFilterDialog extends Dialog {

  int[] filterValues;
  Spinner[] spinners = new Spinner[9];
  Spinner denom;
  CameraFilterDialog dialog = this;
  
  public CameraFilterDialog(Context context, int[] vals) {
    super(context);
    filterValues = vals;
    
    setContentView(R.layout.dialog);
    setTitle(R.string.filter_dialog_title);
    setCancelable(true);
    
    // Initialize the first row of spinners
    int resid;
    for(int i=0; i<3; i++) {
      resid = context.getResources().getIdentifier("k"+i, "id", context.getPackageName());
      spinners[i] = (Spinner)findViewById(resid);
      spinners[i].setSelection(20 - vals[i]);
    }
    
    // Initialize the second row of spinners
    for(int i=3; i<6; i++) {
      resid = context.getResources().getIdentifier("k"+i, "id", context.getPackageName());
      spinners[i] = (Spinner)findViewById(resid);
      spinners[i].setSelection(20 - vals[i+1]);
    }
    
    // Initialize the third row of spinners
    for(int i=6; i<9; i++) {
      resid = context.getResources().getIdentifier("k"+i, "id", context.getPackageName());
      spinners[i] = (Spinner)findViewById(resid);
      spinners[i].setSelection(20 - vals[i+2]);
    }
    
    denom = (Spinner)findViewById(R.id.denom);
    denom.setSelection(20 - vals[11]);
    
    // Configure ok button
    Button okButton = (Button)findViewById(R.id.ok);
    okButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        
        // Update spinner values
        for(int i=0; i<3; i++)
          filterValues[i] = 20 - spinners[i].getSelectedItemPosition();
        for(int i=3; i<6; i++)
          filterValues[i+1] = 20 - spinners[i].getSelectedItemPosition();
        for(int i=6; i<9; i++)
          filterValues[i+2] = 20 - spinners[i].getSelectedItemPosition();
        
        // Update denominator
        filterValues[11] = 20 - denom.getSelectedItemPosition();
        
        dialog.dismiss();
      }
    });
    
    // Configure cancel button
    Button cancelButton = (Button)findViewById(R.id.cancel);
    cancelButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dialog.dismiss();
      }
    });
  }
}
