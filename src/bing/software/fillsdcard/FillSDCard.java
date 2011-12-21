package bing.software.fillsdcard;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class FillSDCard extends Activity {
	private final String TAG = "bing";
	private final String mKB = "abcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwx";
	private final byte[] mBytesOfKB = mKB.getBytes();
	private final String mTmpFolderName = ".tmpfolderWEDFRVCVBHYUMMNFDHJI";
	private final int mLeftSizeWithPermission = 0;
	private boolean mExternalStorageAvailable = false;
	private boolean mExternalStorageWriteable = false;
	private int mFillSize;
	private String mTmpFolderPath;
	private String mSDCardPath;
	private int mTotalSize;
	private int mAvailableSize;
	private int mSingleBlockSize;
	private FileOutputStream mOutputStream;
	private BufferedOutputStream mBuff;
	private ProgressDialog mProgressDialog;

	private TextView mTvLeftStorageSpace;
	private EditText mEtFillWithSize;
	private EditText mEtFillWithPercent;
	private Button mBtnFill;
	private Button mBtnClean;
	private Button mBtnReset;
	private Button mBtnVerify;
	private RadioButton mRbtnFillWithSize;
	private RadioButton mRbtnFillWithPercent;
	private final int FILL_ON = 0xFFFDCA;
	private final int FILL_STOP = 0xFFFDCE;
	private int FILL_STATUS = FILL_STOP;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        init();
        
        mTvLeftStorageSpace = (TextView)findViewById(R.id.leftStorageSpace);
        mRbtnFillWithSize = (RadioButton)findViewById(R.id.rbtnFillWithSize);
        mRbtnFillWithPercent = (RadioButton)findViewById(R.id.rbtnFillWithPercent);
        
        mEtFillWithSize = (EditText)findViewById(R.id.etfillWithSize);
        mEtFillWithPercent = (EditText)findViewById(R.id.etfillWithPercent);
        
        mRbtnFillWithSize.setOnClickListener(radio_listener);
        mRbtnFillWithPercent.setOnClickListener(radio_listener);
        
        mBtnFill = (Button)findViewById(R.id.btnFill);
        mBtnClean = (Button)findViewById(R.id.btnClean);
        mBtnReset = (Button)findViewById(R.id.btnReset);
        mBtnVerify = (Button)findViewById(R.id.btnVerify);
        
        
        // Judge whether SD card is available
        if(!isSDCardAvailable() || !generateTmpFolder()){
        	int rId = !isSDCardAvailable() ? R.string.sdCardUnavailable: R.string.tmpFolderCreatedFailed;
        	String problem = getResources().getString(rId);
        	mTvLeftStorageSpace.setText(problem);
        	setAllItemsDisabled();
        }else{
        	// Continue creating folder and else..
        	getStateOfStorage();
        	
        	mBtnVerify.setOnClickListener(new OnClickListener(){
        		@Override
        		public void onClick(View v) {
        			onVerifyButtonClicked();
        		}
        	});
            
            // Fill SD card button is only available after clicking Verify Data button
            mBtnFill.setEnabled(false);
            mBtnFill.setOnClickListener(new OnClickListener(){
            	@Override
            	public void onClick(View v) {
            		onFillButtonClicked();
            	}    	
            });
            
            mBtnReset.setOnClickListener(new OnClickListener(){
            	@Override
            	public void onClick(View v) {
            		onResetButtonClicked();
            	}
            });
            
            mBtnClean.setOnClickListener(new OnClickListener(){
            	@Override
            	public void onClick(View v) {
                	AlertDialog.Builder builder = new AlertDialog.Builder(FillSDCard.this);
                	builder.setMessage(getString(R.string.alertDialogTitle))
                	       .setCancelable(true)
                	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                	           public void onClick(DialogInterface dialog, int id) {
                	        	   onCleanButtonClicked();
                	           }
                	       })
                	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
                	           public void onClick(DialogInterface dialog, int id) {
                	                dialog.cancel();
                	           }
                	       }).create().show();
            	}
            });
        }
    }
    
    public void onStart(){
    	super.onStart();
    	if(!isSDCardAvailable() || !generateTmpFolder()){
        	int rId = !isSDCardAvailable() ? R.string.sdCardUnavailable: R.string.tmpFolderCreatedFailed;
        	String problem = getResources().getString(rId);
        	mTvLeftStorageSpace.setText(problem);
        	setAllItemsDisabled();
        }
    }
    
    
    private OnClickListener radio_listener = new OnClickListener() {
        public void onClick(View v) {
            // Perform action on clicks
            if(mRbtnFillWithSize.isChecked()){
            	mEtFillWithSize.setEnabled(true);
            	mEtFillWithPercent.setEnabled(false);
            }else{
            	mEtFillWithPercent.setEnabled(true);
            	mEtFillWithSize.setEnabled(false);
            }
        }
    };

    /**
     * Remove files and temperate folder
     * Get current storage information
    */
    private void onCleanButtonClicked(){
    	File tmpFolder = new File(mTmpFolderPath);
		if(tmpFolder.exists()){
			File[] files = tmpFolder.listFiles();
			int count = files.length;
			for(int i = 0; i < count; i++){
				files[i].delete();
			}
//			isReadyToRemoveFolder = true;
//			tmpFolder.delete();
		}
		getStateOfStorage();
        if(mAvailableSize > mLeftSizeWithPermission){
        	mTvLeftStorageSpace.setText(String.format("After delete, current free size: %dKB.", 
        			mAvailableSize));
        }else{
        	mTvLeftStorageSpace.setText(String.format("Current free size: %dKB. It's small enough.", 
        			mAvailableSize));
        }
    }
    /**
     * Generate temperate folder
     * If Edit Text is empty, fill SD Card full to 400(KB) left
     * If Edit Text is filled, fill SD Card with available number(KB)
    */
    private void onVerifyButtonClicked(){
    	EditText etFetchData = mRbtnFillWithSize.isChecked()? mEtFillWithSize: mEtFillWithPercent;
    	// Set fillSize
		if(etFetchData.getText().length() == 0){
        	new Toast(getApplication());
			Toast.makeText(getApplication(), "We will fill SD Card automatically if you left Edit text empty.", 
					Toast.LENGTH_SHORT).show();
			mFillSize = mAvailableSize - mLeftSizeWithPermission;
			if(mFillSize > 0){
				mBtnFill.setEnabled(true);
				mTvLeftStorageSpace.setText(String.format("We will fill SD Card with %dKB\n" +
						"Left storage space is %dKB\n" +
						"Please wait for a while and not quit.", 
						mFillSize, mLeftSizeWithPermission));
			}
			else{
				mBtnFill.setEnabled(false);
				mTvLeftStorageSpace.setText(String.format("Current free size: %dKB, It's small enough.", 
						mAvailableSize));
			}
        }else{
        	if(mRbtnFillWithSize.isChecked()){
        		mFillSize = Integer.parseInt(etFetchData.getText().toString());
        	}else{
        		int filledSize = mTotalSize - mAvailableSize;
        		double fillSize = 0.01 * mTotalSize * (100 - Integer.parseInt(etFetchData.getText().toString()));
        		if(fillSize <= filledSize){
        			mFillSize = 0;
        		}else if(fillSize > filledSize && fillSize < mTotalSize){
        			mFillSize = (int)Math.ceil(fillSize) - filledSize;
        		}else{
        			mFillSize = mAvailableSize;
        		}
        	}
        	if(mFillSize > (mAvailableSize - mLeftSizeWithPermission)){
        		mTvLeftStorageSpace.setText("Sorry, the number is too large to fill");
        		mBtnFill.setEnabled(false);
        	}else{
        		mFillSize = (int)Math.ceil((double)mFillSize / mSingleBlockSize) * mSingleBlockSize;
//        		etFetchData.setText(String.valueOf(mFillSize));
        		mBtnFill.setEnabled(true);
        		mTvLeftStorageSpace.setText(String.format("We will fill SD Card with %dKB as filled in Edit text\n" +
						"Left storage space is %dKB%n" +
						"Left storage percent is %f%%%n" +
						"Please wait for a while and not quit.", 
						mFillSize, 
						mAvailableSize - mFillSize,
						(mAvailableSize - mFillSize)*100f/mTotalSize
						));
        	}
        }
		etFetchData.setEnabled(false);
		mRbtnFillWithSize.setEnabled(false);
		mRbtnFillWithPercent.setEnabled(false);
		mBtnVerify.setEnabled(false);
        Log.d(TAG, String.format("Fill Size: %dKB", mFillSize)); 
        Log.d(TAG, String.format("Left Size: %dKB", mAvailableSize - mFillSize)); 
    }
    /**
     * Fill SD card with one file
     * Set KB with one thousand bytes and fill the file with lines which equals fillSize
    */
    private void onFillButtonClicked() {
		mBtnFill.setEnabled(false);
		mProgressDialog = new ProgressDialog(FillSDCard.this);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setMessage(getResources().getString(R.string.progressbarTitle));
		mProgressDialog.setMax(mFillSize);
		mProgressDialog.setCancelable(true);
		mProgressDialog.show();
		new Thread(new mThread(mHandler)).start();
	   
	   mBtnVerify.setEnabled(true);
	   mEtFillWithSize.setEnabled(true);
	   mEtFillWithSize.setText("");
    }
    
	class mThread extends Thread{
		Handler handler;
		public mThread(Handler h){
			handler = h;
		}
        public void run() {
    		try{            
				mOutputStream = null;
				mBuff = null;
				Log.w(TAG, "Fill size when filling: " + mFillSize);
				File myFile = new File(mTmpFolderPath + File.separator + System.currentTimeMillis());
				
				mOutputStream = new FileOutputStream(myFile);
				mBuff = new BufferedOutputStream(mOutputStream);
				FILL_STATUS = FILL_ON;
				for(int i = 0; i < mFillSize && FILL_STATUS == FILL_ON; i++){
					Log.i(TAG, "FILL_STAUS: " + FILL_STATUS);
					mBuff.write(mBytesOfKB);
					handler.sendEmptyMessage(i+1);
		        	if(!mProgressDialog.isShowing()){
		        		FILL_STATUS = FILL_STOP;
		        	}
				}
				mBuff.flush();
				mBuff.close();
				mOutputStream.close();
				
    			FILL_STATUS = FILL_STOP;
    			handler.sendEmptyMessage(FILL_STATUS);
    		}catch(Exception e){
    			Log.v("_Error_", e.getMessage());
    		}
      }
   }
   
	private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
        	mProgressDialog.setProgress(msg.what);
            switch(msg.what){
            case FILL_STOP:
            	if(mProgressDialog.isShowing()){
            		mProgressDialog.dismiss();
            	}
            	getStateOfStorage();
				mTvLeftStorageSpace.setText(String.format("After filling, current free size: %dKB.", 
						mAvailableSize));
                break;
            default:
//            	getStateOfStorage();
                break;
            }
        }
    };
    
    /**
     * Set Fill button disabled
     * Set Verify data enabled
     * Clean content of Edit Text
     * Set Edit Text enabled
     * Get current storage information
     */
    private void onResetButtonClicked() {
    	EditText et;
    	et = mRbtnFillWithSize.isChecked()? mEtFillWithSize: mEtFillWithPercent;
    	et.setEnabled(true);
    	et.setText("");
    	mBtnFill.setEnabled(false);
    	mBtnVerify.setEnabled(true);
		mRbtnFillWithSize.setEnabled(true);
		mRbtnFillWithPercent.setEnabled(true);
    	getStateOfStorage();
    }
    private void setAllItemsDisabled() {
    	mTvLeftStorageSpace.setText("Please check your SD card. It's not available now.");
    	mEtFillWithSize.setEnabled(false);
    	mEtFillWithPercent.setEnabled(false);
    	mBtnFill.setEnabled(false);
    	mBtnClean.setEnabled(false);
    	mBtnReset.setEnabled(false);
    	mBtnVerify.setEnabled(false);
    	mRbtnFillWithSize.setEnabled(false);
    	mRbtnFillWithPercent.setEnabled(false);
	}
	private boolean isSDCardAvailable() {
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
        	mBtnFill.setEnabled(false);
        	mBtnClean.setEnabled(false);
        	return false;
        }else{
        	return true;
        }
	}

    
    private boolean generateTmpFolder(){
        File tmpFolder = new File(mTmpFolderPath);
        
        boolean isTempFolderReady = false;
        if(!tmpFolder.exists()){
        	isTempFolderReady = tmpFolder.mkdirs();
        }else{
        	isTempFolderReady = true;
        }

        return isTempFolderReady;
    }
    
    private void getStateOfStorage(){
        StatFs SDCardInfo = new StatFs(mSDCardPath);
        int totalBlock = SDCardInfo.getBlockCount();
        int blockSize = SDCardInfo.getBlockSize() / 1024; //KB
//        int freeBlocks = SDCardInfo.getFreeBlocks();
        int availableBlocks = SDCardInfo.getAvailableBlocks();
        mAvailableSize = blockSize * availableBlocks;
        mTotalSize = blockSize * totalBlock;
        Log.d(TAG, "single block Size: " + blockSize);
        Log.d(TAG, String.format("free size: %d KB.", mAvailableSize));
        if(mAvailableSize > mLeftSizeWithPermission){
        	mTvLeftStorageSpace.setText(String.format(
        			"Current free size: %fGB%n" +
        			"Current free size: %fMB%n" +
        			"Current free size: %dKB%n" +
        			"Current filled: %f%%%n" +
        			"Total size: %dKB%n"+
        			"Default fill size: %dKB%n"+
        			"Minimum fill size (single block size): %dKB",
        			mAvailableSize / 1024f / 1024, 
        			mAvailableSize / 1024f, 
        			mAvailableSize, 
        			100 - availableBlocks * 100f/totalBlock,
        			mTotalSize,
        			mAvailableSize - mLeftSizeWithPermission,
        			mSingleBlockSize
        			));
        }else{
        	mTvLeftStorageSpace.setText(String.format("Current free size: %dKB. It's small enough.", 
        			mAvailableSize));
        }
    }
    
    private int getSingleBlockSize(){
    	StatFs SDCardInfo = new StatFs(mSDCardPath);
    	int blockSize = SDCardInfo.getBlockSize() / 1024; //KB
    	Log.d(TAG, "getBlockCount: " + SDCardInfo.getBlockCount() * blockSize / 1024); //MB
    	Log.d(TAG, "getAvailableBlocks: " + SDCardInfo.getAvailableBlocks() * blockSize / 1024);
    	Log.d(TAG, "getFreeBlocks: " + SDCardInfo.getFreeBlocks() * blockSize / 1024);
    	Log.d(TAG, "getBlockCount: " + SDCardInfo.getBlockCount()); //B
    	Log.d(TAG, "getAvailableBlocks: " + SDCardInfo.getAvailableBlocks());
    	Log.d(TAG, "getFreeBlocks: " + SDCardInfo.getFreeBlocks());
    	Log.d(TAG, "getBlockSize: " + SDCardInfo.getBlockSize());
        return SDCardInfo.getBlockSize();
    }
    
    private void init(){
    	mSDCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        mTmpFolderPath= new File(mSDCardPath + File.separator + mTmpFolderName).getAbsolutePath();
        Log.w(TAG, String.format("tmpFolderPath: %s", mTmpFolderPath)); 
        mSingleBlockSize = getSingleBlockSize()/1024;
        Log.d(TAG, "at first block size: " + mSingleBlockSize);
    }
    
    
}