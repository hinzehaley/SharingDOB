package hinzehaley.com.sharedob;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import android.app.DatePickerDialog;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import hinzehaley.com.sharedob.Connection.BirthdayConnection;


public class BirthdaySharingActivity extends AppCompatActivity implements NsdListener {

    NsdHelper mNsdHelper;

    private TextView txtBirthdayView;
    private TextView txtBirthdayName;
    private TextView txtBirthdayAge;
    private TextView txtSharedBirthdayMessage;
    final DateTimeFormatter dtf = DateTimeFormat.forPattern("MMM dd, yyyy");
    private EditText etEnterBirthday;
    private DatePickerDialog datePickerDialog;

    private Handler mUpdateHandler;
    private Button btnShare;

    private ProgressDialogFragment progressDialog;

    private boolean isResumed = false;
    private boolean isConnected = false;
    public static final String TAG = "NsdBirthdays";

    BirthdayConnection mBirthdayConnection;

    /**
     * Gets references to UI objects. Creates a Handler to deal with incoming messages
     * and sets up UI objects
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birthday_sharing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtBirthdayView = (TextView) findViewById(R.id.txt_shared_birthday);
        txtBirthdayName = (TextView) findViewById(R.id.txt_shared_birthday_name);
        txtBirthdayAge = (TextView) findViewById(R.id.txt_shared_birthday_age);
        txtSharedBirthdayMessage = (TextView) findViewById(R.id.txt_shared_birthday_message);

        btnShare = (Button) findViewById(R.id.btn_share_birthday);
        //If Share is clicked, the birthday will be shared
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareBirthday();
            }
        });

        createUpdateHandler();
        setUpDateEditText();
        setUpDatePickerDialog();


    }

    /**
     * Creates a handler. When called, the Handler will handle the incoming
     * Message by adding the birthday to the view
     */
    private void createUpdateHandler(){
        mUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                String birthday = message.getData().getString(Constants.BIRTHDAY_KEY);
                boolean isSender = message.getData().getBoolean(Constants.IS_SENDER_KEY);

                addBirthday(birthday, isSender);
            }
        };
    }



    /**
     * Creates a DatePickerDialog. Sets up an OnDateSetListener to read the date from the dialog
     * When the user has successfully selected a date, formats that date and displays it to user.
     */
    private void setUpDatePickerDialog(){
        LocalDate curDate = LocalDate.now();
        datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener()  {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                LocalDate date = new LocalDate(year, monthOfYear+1, dayOfMonth);
                String str = date.toString(dtf);
                etEnterBirthday.setText(str);
            }

        },curDate.getYear(), curDate.getMonthOfYear()-1, curDate.getDayOfMonth());

    }


    /**
     * Sets up EditText to take no input and open the datePickerDialog when clicked
     */
    private void setUpDateEditText(){
        etEnterBirthday = (EditText) findViewById(R.id.et_select_birth_date);
        etEnterBirthday.setInputType(InputType.TYPE_NULL);
        etEnterBirthday.requestFocus();
        etEnterBirthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerDialog.show();
            }
        });
    }


    /**
     * Registers a service to share and receive birthdays
     */
    public void register(){
        dismissDialog();
        if(mNsdHelper != null && mBirthdayConnection != null) {
            if (mBirthdayConnection.getLocalPort() > -1) {
                mNsdHelper.registerService(mBirthdayConnection.getLocalPort());
                Log.d(TAG, "Registering service");

            } else {
                //If unable to register, waits and tries again
                showDialog();
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        register();
                    }
                }, 500);
            }
        }
    }


    /**
     * Attempts to connect to a service
     */
    public void connect(){
        if(mNsdHelper != null) {
            NsdServiceInfo chosenService = mNsdHelper.getChosenServiceInfo();
            if (chosenService != null) {
                dismissDialog();
                mBirthdayConnection.connectToServer(chosenService.getHost(),
                        chosenService.getPort());
            } else if (!isConnected){
                showDialog();
                //If no service is available yet, keeps trying to connect
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connect();
                    }
                }, 1000);
            }
        }
    }

    /**
     * Called when "Refresh Birthday Sharing" Button is clicked.
     * Registers service again and attempts to discover services.
     * @param v
     */
    public void clickRefresh(View v){
        discover();
        register();
    }

    /**
     * Attempts to discover services for birthday sharing
     */
    public void discover(){
        mNsdHelper.discoverServices();
    }

    /**
     * Sends birthday and name over mBirthdayConnection. If birthday or name weren't provided,
     * doesn't send and provides error message
     */
    public void shareBirthday(){
        EditText etBirthday = (EditText) findViewById(R.id.et_select_birth_date);
        String birthday = etBirthday.getText().toString();
        EditText etName = (EditText) findViewById(R.id.et_name);
        String name = etName.getText().toString();

        if(birthday == null){
            showErrorDialog();
            return;
        }else if (birthday.equals("")){
            showErrorDialog();
            return;
        }else if(name == null){
            showErrorDialog();
            return;
        }else if(name.equals("")){
            showErrorDialog();
            return;
        }

        String birthdayInfoString = birthday + ":" + name;
        mBirthdayConnection.sendBirthday(birthdayInfoString);
    }

    /**
     * Shows an AlertDialog prompting user to fill in fields
     */
    private void showErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.must_fill_in_fields))
                .setCancelable(false)
                .setPositiveButton("OK", null);
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Called when birthday is sent or received. Sets relevant TextViews to display birthday information
     * @param birthday String containing birthday and name separated by colon
     * @param isSender true if the birthday was sent from device, false if sent to device
     */
    public void addBirthday(String birthday, boolean isSender) {
        if(birthday != null){
            if(!birthday.equals("")){
                Log.d(TAG, "Birthday is: " + birthday);
                String[] separated = birthday.split(":");
                String birthdayString = separated[0];
                String nameString = separated[1];
                int age = calculateAge(birthdayString);

                txtBirthdayView.setText(getString(R.string.birthday) +" " + birthdayString);
                txtBirthdayAge.setText(getString(R.string.age) + " " + age);
                txtBirthdayName.setText(getString(R.string.name) + " " + nameString);

                if(isSender){
                    txtSharedBirthdayMessage.setText(getResources().getString(R.string.you_shared_birthday));
                }else{
                    txtSharedBirthdayMessage.setText(getResources().getString(R.string.someone_shared_birthday));
                }
            }
        }
    }

    /**
     * Calulates age of user based on a String containing the formatted birthday.
     * Uses Joda for calculations
     * @param birthdayString
     * @return age
     */
    private int calculateAge(String birthdayString){
        DateTime dateTime = dtf.parseDateTime(birthdayString);
        LocalDate birthdate = new LocalDate(dateTime);
        LocalDate now = new LocalDate();
        Years age = Years.yearsBetween(birthdate, now);

        return age.getYears();

    }

    /**
     * Creates a new BirthdayConnection,
     */
    @Override
    protected void onStart() {
        Log.d(TAG, "Starting.");
        mBirthdayConnection = new BirthdayConnection(mUpdateHandler, this);
        mNsdHelper = new NsdHelper(this, this);
        mNsdHelper.initializeNsd();
        super.onStart();


    }


    /**
     * Stops trying to discover birthday sharing services. Dismisses progressDialog if
     * it is visible
     */
    @Override
    protected void onPause() {
        Log.d(TAG, "Pausing.");
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
        }
        isResumed = false;
        dismissDialog();
        super.onPause();
    }

    /**
     * Attempts to discover, register, and connect with devices
     */
    @Override
    protected void onResume() {
        Log.d(TAG, "Resuming.");
        super.onResume();
        isResumed = true;
        if (mNsdHelper != null) {
            discover();
            register();
            connect();
        }else{
            Log.d(TAG, "Resuming. mNsdHelper is null");

        }
    }


    /**
     * OnDestroy() may not ever be called, so gets rid of the
     * connection here
     */

    @Override
    protected void onStop() {
        Log.d(TAG, "Being stopped.");
        mNsdHelper.tearDown();
        mBirthdayConnection.tearDown();
        isConnected = false;
        mNsdHelper = null;
        mBirthdayConnection = null;
        super.onStop();
    }


    /**
     * If the correct service was found, connects to it
     * @param serviceFound
     */
    @Override
    public void foundService(boolean serviceFound) {
        if(serviceFound) {
            connect();
        }
    }

    /**
     * If the service being connected to was dropped, attempts to discover a new service
     */
    @Override
    public void droppedService() {
        if(isResumed && !isConnected) {
            discover();
        }
    }

    /**
     * Shows a progressDialog if a service is being resolved
     */
    @Override
    public void resolvingService() {
        showDialog();
    }

    /**
     * If the device dropped its own service, re-registers it
     */
    @Override
    public void droppedOwnService() {
        if(isResumed) {
            register();
        }
    }

    /**
     * Once connected to a device, stops trying to discover new services
     * and dismisses progressDialog
     */
    @Override
    public void isConnected() {
        if(isResumed){
            isConnected = true;
            mNsdHelper.stopDiscovery();
            dismissDialog();
        }
    }


    /**
     * Shows progressDialog. If it doesn't exist yet, creates it
     */
    public void showDialog() {
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        progressDialog = (ProgressDialogFragment) fragmentManager.findFragmentByTag("ProgressDialog");
        if(progressDialog == null) {
            progressDialog = new ProgressDialogFragment();
        }
        if(!progressDialog.isAdded()) {
            progressDialog.show(fragmentManager, "ProgressDialog");
        }
    }

    /**
     * Dismisses progressDialog if it is visible
     */
    public void dismissDialog(){
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();

        progressDialog = (ProgressDialogFragment) fragmentManager.findFragmentByTag("ProgressDialog");
        if(progressDialog != null){
            progressDialog.dismiss();
        }

    }


}

