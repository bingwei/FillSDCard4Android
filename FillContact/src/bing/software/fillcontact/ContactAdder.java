/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bing.software.fillcontact;

import java.util.ArrayList;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.OnAccountsUpdateListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;


public final class ContactAdder extends Activity implements OnAccountsUpdateListener
{
    public static final String TAG = "bing";
    private final int phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
    private final int emailType = ContactsContract.CommonDataKinds.Email.TYPE_MOBILE;
    private ArrayList<AccountData> mAccounts;
    private AccountAdapter mAccountAdapter;
    private Spinner mAccountSpinner;
    private EditText mContactAmountEditText;
    private EditText mContactNameDigitEditText;
    private EditText mContactPhoneDigitEditText;
    private Button mContactGenerateButton;
    private Button mContactRemoveAllButton;
    private AccountData mSelectedAccount;
    private boolean mAddEmail;
    private CheckBox mAddEmailControl;
    static final int GENERATE_PROGRESS_DIALOG = 0xFFDE;
    static final int REMOVE_PROGRESS_DIALOG = 0xFFDB;
    private GenerateProgressThread generateProgressThread;
    private ProgressDialog mGenerateProgressDialog;
    private ProgressDialog mCleanProgressDialog;
    private int contactAmount;
    private int nameDigit;
    private int phoneDigit;
    private final int REMOVE_ON = 1;
    private final int REMOVE_STOP = 0;
    private int REMOVE_STATUS = REMOVE_ON;

    /**
     * Called when the activity is first created. Responsible for initializing the UI.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.v(TAG, "Activity State: onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_adder);

        // Obtain handles to UI objects
        mAccountSpinner = (Spinner) findViewById(R.id.accountSpinner);
        mContactAmountEditText = (EditText) findViewById(R.id.contactAmountEditText);
        mContactNameDigitEditText = (EditText) findViewById(R.id.contactNameDigitEditText);
        mContactPhoneDigitEditText = (EditText) findViewById(R.id.contactPhoneDigitEditText);
        mContactGenerateButton = (Button) findViewById(R.id.contactGenerateButton);
        mContactRemoveAllButton = (Button) findViewById(R.id.contactRemoveAllButton);
        mAddEmail = false;
        mAddEmailControl = (CheckBox) findViewById(R.id.addEmail);


        // Prepare model for account spinner
        mAccounts = new ArrayList<AccountData>();
        mAccountAdapter = new AccountAdapter(this, mAccounts);
        mAccountSpinner.setAdapter(mAccountAdapter);


        // Prepare the system account manager. On registering the listener below, we also ask for
        // an initial callback to pre-populate the account list.
        AccountManager.get(this).addOnAccountsUpdatedListener(this, null, true);

        // Register handlers for UI elements
        mAccountSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long i) {
                updateAccountSelection();
            }

            public void onNothingSelected(AdapterView<?> parent) {
                // We don't need to worry about nothing being selected, since Spinners don't allow
                // this.
            }
        });
        mContactGenerateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGenerateButtonClicked();
            }
        });
        
        mContactRemoveAllButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	AlertDialog.Builder builder = new AlertDialog.Builder(ContactAdder.this);
            	builder.setMessage(ContactAdder.this.getString(R.string.alertDialogTitle))
            	       .setCancelable(true)
            	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	        	   showDialog(REMOVE_PROGRESS_DIALOG);
            	           }
            	       })
            	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	                dialog.cancel();
            	           }
            	       }).create().show();
            }
        });
        
        mAddEmailControl.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "mShowInvisibleControl changed: " + isChecked);
                mAddEmail = isChecked;
            }
        });
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event){
    	Log.d(TAG, "KeyEvent is " + event);
    	if(REMOVE_STATUS == REMOVE_ON && keyCode == KeyEvent.KEYCODE_BACK){
    		REMOVE_STATUS = REMOVE_STOP;
    	}
    	return super.onKeyUp(keyCode, event);
    }
    
    

    /**
     * Actions for when the Save button is clicked. Creates a contact entry and terminates the
     * activity.
     */
    private void onGenerateButtonClicked() {
        Log.v(TAG, "Save button clicked");
        createContactEntry();
    }
    
    


    /**
     * Creates a contact entry from the current UI values in the account named by mSelectedAccount.
     */
    protected void createContactEntry() {
        // Get value from edit text. If empty, then select hint value as default
        if(mContactAmountEditText.getText().toString().length() != 0){
        	contactAmount = Integer.parseInt(mContactAmountEditText.getText().toString());
        }else{
        	contactAmount = Integer.parseInt(mContactAmountEditText.getHint().toString());
        }
        if(mContactNameDigitEditText.getText().toString().length() != 0){
        	nameDigit = Integer.parseInt(mContactNameDigitEditText.getText().toString());
        }else{
        	nameDigit = Integer.parseInt(mContactNameDigitEditText.getHint().toString());
        }
        if(mContactPhoneDigitEditText.getText().toString().length() != 0){
        	phoneDigit = Integer.parseInt(mContactPhoneDigitEditText.getText().toString());
        }else{
        	phoneDigit = Integer.parseInt(mContactPhoneDigitEditText.getHint().toString());
        }
        
        Log.i(TAG,"Selected account: " + mSelectedAccount.getName() + " (" +
    			mSelectedAccount.getType() + ")");
        Log.i(TAG,"Number sets: " + "Contacts amount: " + contactAmount);
        Log.i(TAG,"Number sets: " + "Name digit: " + nameDigit);
        Log.i(TAG,"Number sets: " + "Phone digit: " + phoneDigit);
        // Prepare contact creation request
        //
        // Note: We use RawContacts because this data must be associated with a particular account.
        //       The system will aggregate this with any other data for this contact and create a
        //       coresponding entry in the ContactsContract.Contacts provider for us.
        showDialog(GENERATE_PROGRESS_DIALOG);
    }

    /**
     * Called when this activity is about to be destroyed by the system.
     */
    @Override
    public void onDestroy() {
        // Remove AccountManager callback
        AccountManager.get(this).removeOnAccountsUpdatedListener(this);
        super.onDestroy();
    }
    
    /**
     * Called when invoking showDialog()
     */
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case GENERATE_PROGRESS_DIALOG:
        	mGenerateProgressDialog = new ProgressDialog(this);
            mGenerateProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mGenerateProgressDialog.setMessage(ContactAdder.this.getString(R.string.generateProgressDialogTitle));
            mGenerateProgressDialog.setCancelable(true);
            return mGenerateProgressDialog;
        case REMOVE_PROGRESS_DIALOG:
        	mCleanProgressDialog = new ProgressDialog(this);
        	mCleanProgressDialog.setMessage(ContactAdder.this.getString(R.string.removeProgressDialogTitle));
        	mCleanProgressDialog.setCancelable(true);
        	return mCleanProgressDialog;
        default:
            return null;
        }
    }

    /**
     * Called when invoking onCreateDialog(int id)
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
        case GENERATE_PROGRESS_DIALOG:
        	mGenerateProgressDialog.setMax(contactAmount);
        	mGenerateProgressDialog.setProgress(0);
            generateProgressThread = new GenerateProgressThread(mGenerateHandler, contactAmount, nameDigit, phoneDigit, mAddEmail);
            generateProgressThread.start();
            break;
        case REMOVE_PROGRESS_DIALOG:
        	new Thread(new CleanThread(mCleanHandler)).start();
        	break;
        default:
            break;
        }
    }
    
    private Handler mGenerateHandler = new Handler(){
        public void handleMessage(Message msg) {
            int current = msg.arg1;
            Log.d(TAG, "current is " + current);
            mGenerateProgressDialog.setProgress(current);
            if (current >= contactAmount){
            	mGenerateProgressDialog.dismiss();
                ContactAdder.this.finish();
            }
        }
    };
    
    /**
     * New thread used to generate contact
     */
    class GenerateProgressThread extends Thread{
        private String name = null;
        private String phone = null;
        private String email = null;
        private Handler mHandler = null;
        private int contactAmount = 0;
        private int nameDigit = 0;
        private int phoneDigit = 0;
        private boolean mAddEmail = false;
        private ArrayList<ContentProviderOperation> ops = null;
        final static int STATE_DONE = 0;
        final static int STATE_RUNNING = 1;

        public GenerateProgressThread(Handler h, int contactAmount, int nameDigit, int phoneDigit, boolean mAddEmail){
        	this.mHandler = h;
        	this.contactAmount = contactAmount;
        	this.nameDigit = nameDigit;
        	this.phoneDigit = phoneDigit;
        	this.mAddEmail = mAddEmail;
        }
    	@Override
    	public void run() {
			for(int i = 0; i < contactAmount; i++){
				try{
					name = Utils.getRandomString(nameDigit);
					phone = Utils.getRandomNumber(phoneDigit);
					email = Utils.getRandomEmail();
					
					ops = new ArrayList<ContentProviderOperation>();
					ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
							.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, mSelectedAccount.getType())
							.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, mSelectedAccount.getName())
							.build());
					ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
							.withValue(ContactsContract.Data.MIMETYPE,
									ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
									.withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, 1)
									.build());
					ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
							.withValue(ContactsContract.Data.MIMETYPE,
									ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
									.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
									.build());
					ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
							.withValue(ContactsContract.Data.MIMETYPE,
									ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
									.withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
									.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
									.build());
					if(mAddEmail){
						ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
								.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
								.withValue(ContactsContract.Data.MIMETYPE,
										ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
										.withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType)
										.withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
										.build());
					}
					
					// Ask the Contact provider to create a new contact
					
					Log.i(TAG,i + ".Creating contact: " + name);
					Log.i(TAG,i + ".Creating phone number: " + phone);
					getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
				}catch(ArrayIndexOutOfBoundsException e){
					// Display warning
//    					Context ctx = getApplicationContext();
					CharSequence txt = getString(R.string.contactDigitFailure);
//    					int duration = Toast.LENGTH_SHORT;
//    					Toast toast = Toast.makeText(ctx, txt, duration);
//    					toast.show();
					
					// Log exception
					Log.e(TAG, "" + txt + e);
				}catch (Exception e) {
					// Display warning
//    					Context ctx = getApplicationContext();
					CharSequence txt = getString(R.string.contactCreationFailure);
//    					int duration = Toast.LENGTH_SHORT;
//    					Toast toast = Toast.makeText(ctx, txt, duration);
//    					toast.show();
					
					// Log exception
					Log.e(TAG, "" + txt + e);
				}
				Message msg = mHandler.obtainMessage();
				msg.arg1 = i+1;
				mHandler.sendMessage(msg);
				if(!mGenerateProgressDialog.isShowing()){
					return ;
				}
    		}
    	}
    }
    
    private Handler mCleanHandler = new Handler() {
        public void handleMessage(Message msg) {
        	switch(msg.what){
        	case REMOVE_STOP:
        		mCleanProgressDialog.dismiss();
        		ContactAdder.this.finish();
        	default:
        		break;
        	}
        }
    };
    
    class CleanThread extends Thread{
    	Handler mHandler;
    	public CleanThread(Handler h){
    		mHandler = h;
    	}
		@Override
		public void run() {
			REMOVE_STATUS = REMOVE_ON;
			ContentResolver cv = getContentResolver();
			Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
			while (cursor.moveToNext() && REMOVE_STATUS == REMOVE_ON) {
		       String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
		       Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
		       cv.delete(uri, null, null);
		       mHandler.sendEmptyMessage(REMOVE_STATUS);
			}
			REMOVE_STATUS = REMOVE_STOP;
			mHandler.sendEmptyMessage(REMOVE_STATUS);
		}
    	
    }

    /**
     * Updates account list spinner when the list of Accounts on the system changes. Satisfies
     * OnAccountsUpdateListener implementation.
     */
    public void onAccountsUpdated(Account[] a) {
        Log.i(TAG, "Account list update detected");
        // Clear out any old data to prevent duplicates
        mAccounts.clear();

        // Get account data from system
        AuthenticatorDescription[] accountTypes = AccountManager.get(this).getAuthenticatorTypes();

        // Populate tables
        for (int i = 0; i < a.length; i++) {
            // The user may have multiple accounts with the same name, so we need to construct a
            // meaningful display name for each.
            String systemAccountType = a[i].type;
            AuthenticatorDescription ad = getAuthenticatorDescription(systemAccountType,
                    accountTypes);
            AccountData data = new AccountData(a[i].name, ad);
            mAccounts.add(data);
        }

        // Update the account spinner
        mAccountAdapter.notifyDataSetChanged();
    }

    /**
     * Obtain the AuthenticatorDescription for a given account type.
     * @param type The account type to locate.
     * @param dictionary An array of AuthenticatorDescriptions, as returned by AccountManager.
     * @return The description for the specified account type.
     */
    private static AuthenticatorDescription getAuthenticatorDescription(String type,
            AuthenticatorDescription[] dictionary) {
        for (int i = 0; i < dictionary.length; i++) {
            if (dictionary[i].type.equals(type)) {
                return dictionary[i];
            }
        }
        // No match found
        throw new RuntimeException("Unable to find matching authenticator");
    }

    /**
     * Update account selection. If NO_ACCOUNT is selected, then we prohibit inserting new contacts.
     */
    private void updateAccountSelection() {
        // Read current account selection
        mSelectedAccount = (AccountData) mAccountSpinner.getSelectedItem();
    }

    /**
     * A container class used to repreresent all known information about an account.
     */
    private class AccountData {
        private String mName;
        private String mType;
        private CharSequence mTypeLabel;
        private Drawable mIcon;

        /**
         * @param name The name of the account. This is usually the user's email address or
         *        username.
         * @param description The description for this account. This will be dictated by the
         *        type of account returned, and can be obtained from the system AccountManager.
         */
        public AccountData(String name, AuthenticatorDescription description) {
            mName = name;
            if (description != null) {
                mType = description.type;

                // The type string is stored in a resource, so we need to convert it into something
                // human readable.
                String packageName = description.packageName;
                PackageManager pm = getPackageManager();

                if (description.labelId != 0) {
                    mTypeLabel = pm.getText(packageName, description.labelId, null);
                    if (mTypeLabel == null) {
                        throw new IllegalArgumentException("LabelID provided, but label not found");
                    }
                } else {
                    mTypeLabel = "";
                }

                if (description.iconId != 0) {
                    mIcon = pm.getDrawable(packageName, description.iconId, null);
                    if (mIcon == null) {
                        throw new IllegalArgumentException("IconID provided, but drawable not " +
                                "found");
                    }
                } else {
                    mIcon = getResources().getDrawable(android.R.drawable.sym_def_app_icon);
                }
            }
        }

        public String getName() {
            return mName;
        }

        public String getType() {
            return mType;
        }

        public CharSequence getTypeLabel() {
            return mTypeLabel;
        }

        public Drawable getIcon() {
            return mIcon;
        }

        public String toString() {
            return mName;
        }
    }

    /**
     * Custom adapter used to display account icons and descriptions in the account spinner.
     */
    private class AccountAdapter extends ArrayAdapter<AccountData> {
        public AccountAdapter(Context context, ArrayList<AccountData> accountData) {
            super(context, android.R.layout.simple_spinner_item, accountData);
            setDropDownViewResource(R.layout.account_entry);
        }

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            // Inflate a view template
            if (convertView == null) {
                LayoutInflater layoutInflater = getLayoutInflater();
                convertView = layoutInflater.inflate(R.layout.account_entry, parent, false);
            }
            TextView firstAccountLine = (TextView) convertView.findViewById(R.id.firstAccountLine);
            TextView secondAccountLine = (TextView) convertView.findViewById(R.id.secondAccountLine);
            ImageView accountIcon = (ImageView) convertView.findViewById(R.id.accountIcon);

            // Populate template
            AccountData data = getItem(position);
            firstAccountLine.setText(data.getName());
            secondAccountLine.setText(data.getTypeLabel());
            Drawable icon = data.getIcon();
            if (icon == null) {
                icon = getResources().getDrawable(android.R.drawable.ic_menu_search);
            }
            accountIcon.setImageDrawable(icon);
            return convertView;
        }
    }
    


}
