package bing.software.fillsdcard;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
/*
* Need to Update:
* 1. Open a independent thread to fill sd card
* 2. Add progress bar to show how much left
* 3. Reconstruct to make it more clear and more reusable
* 4. Add Exceed button to permit to fill SD card full
* 
* Next:
* Tool for show screen of Android device in PC in real time
* DDMS Lib
*/	
public class FillSDCard extends Activity {
	private final String TAG = "bing";
	final String KB = "abcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwx";
	final byte[] BKB = KB.getBytes();
	final String tempFolderName = ".tmpdemoWEDFRVCVBHYUMMNFDHJI";
	final int leftSizeWithPermission = 400;
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;
	int fillSize = 0;
	String tmpFolderPath;
	String SDCardPath;
	int freeSize;
	FileOutputStream outputStream;
	BufferedOutputStream Buff;
	ProgressDialog progressDialog;

	TextView tvLeftStorageSpace;
	EditText etFillSize;
	Button btnFillSDCard;
	Button btnCleanTempFiles;
	Button btnReset;
	Button btnVerifyData;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        tvLeftStorageSpace = (TextView)findViewById(R.id.leftStorageSpace);
        etFillSize = (EditText)findViewById(R.id.fillSize);
        btnFillSDCard = (Button)findViewById(R.id.btnFillSD);
        // Fill SD card button is only available after clicking Verify Data button
        btnFillSDCard.setEnabled(false);
        btnCleanTempFiles = (Button)findViewById(R.id.btnClean);
        btnReset = (Button)findViewById(R.id.btnReset);
        btnVerifyData = (Button)findViewById(R.id.btnVerifyData);
        
        // Judge whether SD card is available
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        
        if(false == mExternalStorageAvailable || false == mExternalStorageWriteable){
        	btnFillSDCard.setEnabled(false);
        	btnCleanTempFiles.setEnabled(false);
        	tvLeftStorageSpace.setText("Please check your SD card. It's not available now.");
        }

        
        SDCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        tmpFolderPath= new File(SDCardPath + File.separator + tempFolderName).getAbsolutePath(); 
        
        if(!generateTmpFolder()){
        	btnFillSDCard.setEnabled(false);
        	btnCleanTempFiles.setEnabled(false);
        	Log.e(TAG, String.format("Temp folder<%s> meets problem", tempFolderName)); 
        	tvLeftStorageSpace.setText("Please check SD card. Creating folder failed."); 
        }
        
        getStateOfStorage();
        
        
        /**
         * Fill SD card with one file
         * Set KB with one thousand bytes and fill the file with lines which equals fillSize
        */
        btnFillSDCard.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				btnFillSDCard.setEnabled(false);
				progressDialog = ProgressDialog.show(FillSDCard.this, "Please wait", "It is filling SD Card...", true);

				
				new Thread(){ 

			        @Override 

			        public void run() {
		        		try{            
		        			if(generateTmpFolder()){
		        				outputStream = null;
		        				Buff = null;  
		        				Log.w(TAG, "Fill size when filling: " + fillSize);
		        				// TODO Auto-generated method stub
		        				File myFile = new File(tmpFolderPath + File.separator + System.currentTimeMillis());
		        				
		        				outputStream = new FileOutputStream(myFile);
		        				Buff = new BufferedOutputStream(outputStream);
		        				
		        				for(int i = fillSize; i > 0; i--){
		        					Buff.write(KB.getBytes());
		        				}
		        				Buff.flush();
		        				Buff.close();
		        				outputStream.close();
		        			}
		        			handler.sendEmptyMessage(0);
		        			progressDialog.dismiss();
		        		}catch(Exception e){
		        			Log.v("_Error_", e.getMessage());
		        		}
			      }
			   }.start();
			   
		        /*
		         * Set Verify button enabled
		         * Clean content of Edit Text
		         * Set Edit Text enabled
		         * Get current storage information
		        */
			   btnVerifyData.setEnabled(true);
				etFillSize.setEnabled(true);
				etFillSize.setText("");
				getStateOfStorage();
			}    	
        });
        
        /*
         * Generate temperate folder
         * If Edit Text is empty, fill SD Card full to 400(KB) left
         * If Edit Text is filled, fill SD Card with available number(KB)
        */
        btnVerifyData.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
		        // Set fillSize
				if(etFillSize.getText().length() == 0){
		        	new Toast(getApplication());
					Toast.makeText(getApplication(), "We will fill SD Card automatically if you left Edit text empty.", 
							Toast.LENGTH_SHORT).show();
					fillSize = freeSize - leftSizeWithPermission;
					if(fillSize > 0){
						btnFillSDCard.setEnabled(true);
						tvLeftStorageSpace.setText(String.format("We will fill SD Card with %dKB\n" +
								"Left storage space is %dKB\n" +
								"Please wait for a while and not quit.", 
								fillSize, leftSizeWithPermission));
					}
					else{
						btnFillSDCard.setEnabled(false);
						tvLeftStorageSpace.setText(String.format("Current free size: %dKB, It's small enough.", 
								freeSize));
					}
		        }else{
		        	fillSize = Integer.parseInt(etFillSize.getText().toString());
		        	if(fillSize >= (freeSize - leftSizeWithPermission)){
		        		tvLeftStorageSpace.setText("Sorry, the number is too large to fill");
		        		btnFillSDCard.setEnabled(false);
		        	}else{
		        		if(fillSize < 8){
		        			fillSize = 8;
		        		}
		        		btnFillSDCard.setEnabled(true);
		        		tvLeftStorageSpace.setText(String.format("We will fill SD Card with %dKB as filled in Edit text\n" +
								"Left storage space is %dKB\n" +
								"Please wait for a while and not quit.", 
								fillSize, freeSize - fillSize));
		        	}
		        }
				etFillSize.setEnabled(false);
				btnVerifyData.setEnabled(false);
		        Log.d(TAG, String.format("Fill Size: %dKB", fillSize)); 
		        Log.d(TAG, String.format("Left Size: %dKB", freeSize - fillSize)); 
			}
        	
        });
        
        /*
         * Remove files and temperate folder
         * Get current storage information
        */
        btnCleanTempFiles.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				File tmpFolder = new File(tmpFolderPath);
				if(tmpFolder.exists()){
					File[] files = tmpFolder.listFiles();
					int count = files.length;
					for(int i = 0; i < count; i++){
						files[i].delete();
					}
//					isReadyToRemoveFolder = true;
//					tmpFolder.delete();
				}
				getStateOfStorage();
    	        if(freeSize > leftSizeWithPermission){
    	        	tvLeftStorageSpace.setText(String.format("After delete, current free size: %dKB.", 
    	        			freeSize));
		        }else{
		        	tvLeftStorageSpace.setText(String.format("Current free size: %dKB. It's small enough.", 
		        			freeSize));
		        }
			}
        	
        });
        
        /*
         * Set Fill button disabled
         * Set Verify data enabled
         * Clean content of Edit Text
         * Set Edit Text enabled
         * Get current storage information
        */
        btnReset.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				btnFillSDCard.setEnabled(false);
				btnVerifyData.setEnabled(true);
				etFillSize.setEnabled(true);
				etFillSize.setText("");
				getStateOfStorage();
			}
        	
        });
        
    }
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
            case 0:
				getStateOfStorage();
				tvLeftStorageSpace.setText(String.format("After filling, current free size: %dKB.", 
						freeSize));
                break;
            default:
                break;
            }
        }
    };
    
   

    
    public boolean generateTmpFolder(){
		// Generate temperate folder 

        Log.w(TAG, String.format("tmpFolderPath: %s", tmpFolderPath)); 
        
        File tmpFolder = new File(tmpFolderPath);
        
        boolean isTempFolderReady = false;
        if(!tmpFolder.exists()){
        	isTempFolderReady = tmpFolder.mkdirs();
        }else{
        	isTempFolderReady = true;
        }

        return isTempFolderReady;
    }
    
    public void getStateOfStorage(){
        StatFs SDCardInfo = new StatFs(SDCardPath);
        int blockSize = SDCardInfo.getBlockSize();
        int freeBlocks = SDCardInfo.getFreeBlocks();
        freeSize = blockSize * freeBlocks / 1024;
        Log.d(TAG, String.format("free size: %d KB.", freeSize));
        if(freeSize > leftSizeWithPermission){
        	tvLeftStorageSpace.setText(String.format(
        			"Current free size: %dKB\n" +
        			"Default fill size: %dKB", 
        			freeSize, (freeSize - leftSizeWithPermission)));
        }else{
        	tvLeftStorageSpace.setText(String.format("Current free size: %dKB. It's small enough.", 
        			freeSize));
        }
    }
    
    
    
}