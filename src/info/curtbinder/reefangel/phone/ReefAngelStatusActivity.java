package info.curtbinder.reefangel.phone;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ReefAngelStatusActivity extends Activity implements OnClickListener {
	private static final String TAG = "RAStatus";
		
	// Display views
	private View refreshButton;
	private TextView updateTime;
	//private TextView messageText;
	private TextView t1Text;
	private TextView t2Text;
	private TextView t3Text;
	private TextView phText;
	private TextView dpText;
	private TextView apText;
	private TextView salinityText;
	private TextView t1Label;
	private TextView t2Label;
	private TextView t3Label;
	private TextView dpLabel;
	private TextView apLabel;
	private TextView salinityLabel;
	
	private TextView [] mainPortLabels = new TextView[8];
	private ToggleButton [] mainPortBtns = new ToggleButton[8];
	private View [] mainPortMaskBtns = new View[8];
	
	// Threading
	private Handler guiThread;
	private ExecutorService statusThread;
	private Runnable updateTask;
	@SuppressWarnings("rawtypes")
	private Future statusPending;
	private String controllerCommand;
	private boolean updateStatusScreen;
	
	// View visibility
	//private boolean showMessageText;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        findViews();
        initThreading();
               
        updateViewsVisibility();
        
        setOnClickListeners();
    }
    
	@Override
	protected void onPause() {
		super.onPause();
	}


	@Override
	protected void onResume() {
		super.onResume();   
        guiUpdateDisplay();
	}

	private void findViews() {
		refreshButton = findViewById(R.id.refresh_button);
		updateTime = (TextView) findViewById(R.id.updated);
		t1Text = (TextView) findViewById(R.id.temp1);
		t2Text = (TextView) findViewById(R.id.temp2);
		t3Text = (TextView) findViewById(R.id.temp3);
		phText = (TextView) findViewById(R.id.ph);
		dpText = (TextView) findViewById(R.id.dp);
		apText = (TextView) findViewById(R.id.ap);
		salinityText = (TextView) findViewById(R.id.salinity);
		t1Label = (TextView) findViewById(R.id.t1_label);
		t2Label = (TextView) findViewById(R.id.t2_label);
		t3Label = (TextView) findViewById(R.id.t3_label);
		dpLabel = (TextView) findViewById(R.id.dp_label);
		apLabel = (TextView) findViewById(R.id.ap_label);
		salinityLabel = (TextView) findViewById(R.id.salinity_label);

		mainPortLabels[0] = (TextView) findViewById(R.id.main_port1_label);
		mainPortLabels[1] = (TextView) findViewById(R.id.main_port2_label);
		mainPortLabels[2] = (TextView) findViewById(R.id.main_port3_label);
		mainPortLabels[3] = (TextView) findViewById(R.id.main_port4_label);
		mainPortLabels[4] = (TextView) findViewById(R.id.main_port5_label);
		mainPortLabels[5] = (TextView) findViewById(R.id.main_port6_label);
		mainPortLabels[6] = (TextView) findViewById(R.id.main_port7_label);
		mainPortLabels[7] = (TextView) findViewById(R.id.main_port8_label);
		mainPortBtns[0] = (ToggleButton) findViewById(R.id.main_port1);
		mainPortBtns[1] = (ToggleButton) findViewById(R.id.main_port2);
		mainPortBtns[2] = (ToggleButton) findViewById(R.id.main_port3);
		mainPortBtns[3] = (ToggleButton) findViewById(R.id.main_port4);
		mainPortBtns[4] = (ToggleButton) findViewById(R.id.main_port5);
		mainPortBtns[5] = (ToggleButton) findViewById(R.id.main_port6);
		mainPortBtns[6] = (ToggleButton) findViewById(R.id.main_port7);
		mainPortBtns[7] = (ToggleButton) findViewById(R.id.main_port8);
		
		mainPortMaskBtns[0] = findViewById(R.id.main_port1mask);
		mainPortMaskBtns[1] = findViewById(R.id.main_port2mask);
		mainPortMaskBtns[2] = findViewById(R.id.main_port3mask);
		mainPortMaskBtns[3] = findViewById(R.id.main_port4mask);
		mainPortMaskBtns[4] = findViewById(R.id.main_port5mask);
		mainPortMaskBtns[5] = findViewById(R.id.main_port6mask);
		mainPortMaskBtns[6] = findViewById(R.id.main_port7mask);
		mainPortMaskBtns[7] = findViewById(R.id.main_port8mask);
	}
	
	private void setOnClickListeners() {
		refreshButton.setOnClickListener(this);
		for ( int i = 0; i < 8; i++ ) {
			if ( isController() ) {
				mainPortBtns[i].setOnClickListener(this);
				mainPortMaskBtns[i].setOnClickListener(this);
			} else {
				mainPortBtns[i].setClickable(false);
				mainPortMaskBtns[i].setClickable(false);
			}
		}
	}
	
	private void updateViewsVisibility() {
		// updates all the views visibility based on user settings
		// get values from Preferences
        //showMessageText = false;
        
        // Labels
        t1Label.setText(Prefs.getT1Label(getBaseContext()));
        t2Label.setText(Prefs.getT2Label(getBaseContext()));
        t3Label.setText(Prefs.getT3Label(getBaseContext()));
        dpLabel.setText(Prefs.getDPLabel(getBaseContext()));
        apLabel.setText(Prefs.getAPLabel(getBaseContext()));
        
        setMainRelayLabels();
        
        // Visibility
		if ( Prefs.getT2Visibility(getBaseContext()) ) {
			Log.d(TAG, "T2 visible");
			t2Text.setVisibility(View.VISIBLE);
			t2Label.setVisibility(View.VISIBLE);
		} else {
			Log.d(TAG, "T2 gone");
			t2Text.setVisibility(View.GONE);
			t2Label.setVisibility(View.GONE);
		}
		if ( Prefs.getT3Visibility(getBaseContext()) ) {
			Log.d(TAG, "T3 visible");
			t3Text.setVisibility(View.VISIBLE);
			t3Label.setVisibility(View.VISIBLE);
		} else {
			Log.d(TAG, "T3 gone");
			t3Text.setVisibility(View.GONE);
			t3Label.setVisibility(View.GONE);
		}
		if ( Prefs.getDPVisibility(getBaseContext()) ) {
			Log.d(TAG, "DP visible");
			dpText.setVisibility(View.VISIBLE);
			dpLabel.setVisibility(View.VISIBLE);
		} else {
			Log.d(TAG, "DP gone");
			dpText.setVisibility(View.GONE);
			dpLabel.setVisibility(View.GONE);
		}
		if ( Prefs.getAPVisibility(getBaseContext()) ) {
			Log.d(TAG, "AP visible");
			apText.setVisibility(View.VISIBLE);
			apLabel.setVisibility(View.VISIBLE);
		} else {
			Log.d(TAG, "AP gone");
			apText.setVisibility(View.GONE);
			apLabel.setVisibility(View.GONE);
		}
		if ( Prefs.getSalinityVisibility(getBaseContext()) ) {
			Log.d(TAG, "Salinity visible");
			salinityText.setVisibility(View.VISIBLE);
			salinityLabel.setVisibility(View.VISIBLE);
		} else {
			Log.d(TAG, "Salinity gone");
			salinityText.setVisibility(View.GONE);
			salinityLabel.setVisibility(View.GONE);
		}
		//if ( ! showMessageText )
		//	messageText.setVisibility(View.GONE);
	}
	
	private void setMainRelayLabels() {
		for ( int i = 0; i < 8; i++ ) {
			mainPortLabels[i].setText(Prefs.getMainRelayLabel(getBaseContext(), i+1));
		}
	}
	
	@Override
	public void onClick(View v) {
    	switch ( v.getId() ){
    	case R.id.refresh_button:
    		// launch the update
    		Log.d(TAG, "onClick Refresh button");
    		launchStatusTask();
    		break;
    	case R.id.main_port1:
    		Log.d(TAG, "toggle port 1");
    		sendRelayToggleTask(1);
    		break;
    	case R.id.main_port2:
    		Log.d(TAG, "toggle port 2");
    		sendRelayToggleTask(2);
    		break;
    	case R.id.main_port3:
    		Log.d(TAG, "toggle port 3");
    		sendRelayToggleTask(3);
    		break;
    	case R.id.main_port4:
    		Log.d(TAG, "toggle port 4");
    		sendRelayToggleTask(4);
    		break;
    	case R.id.main_port5:
    		Log.d(TAG, "toggle port 5");
    		sendRelayToggleTask(5);
    		break;
    	case R.id.main_port6:
    		Log.d(TAG, "toggle port 6");
    		sendRelayToggleTask(6);
    		break;
    	case R.id.main_port7:
    		Log.d(TAG, "toggle port 7");
    		sendRelayToggleTask(7);
    		break;
    	case R.id.main_port8:
    		Log.d(TAG, "toggle port 8");
    		sendRelayToggleTask(8);
    		break;
    	case R.id.main_port1mask:
    		Log.d(TAG, "clear mask 1");
    		sendRelayClearMaskTask(1);
    		break;
    	case R.id.main_port2mask:
    		Log.d(TAG, "clear mask 2");
    		sendRelayClearMaskTask(2);
    		break;
    	case R.id.main_port3mask:
    		Log.d(TAG, "clear mask 3");
    		sendRelayClearMaskTask(3);
    		break;
    	case R.id.main_port4mask:
    		Log.d(TAG, "clear mask 4");
    		sendRelayClearMaskTask(4);
    		break;
    	case R.id.main_port5mask:
    		Log.d(TAG, "clear mask 5");
    		sendRelayClearMaskTask(5);
    		break;
    	case R.id.main_port6mask:
    		Log.d(TAG, "clear mask 6");
    		sendRelayClearMaskTask(6);
    		break;
    	case R.id.main_port7mask:
    		Log.d(TAG, "clear mask 7");
    		sendRelayClearMaskTask(7);
    		break;
    	case R.id.main_port8mask:
    		Log.d(TAG, "clear mask 8");
    		sendRelayClearMaskTask(8);
    		break;
    	}
	}
	private void sendRelayToggleTask(int port) {
		Log.d(TAG, "sendRelayToggleTask");
		int status = Relay.PORT_STATE_OFF;
		if ( mainPortBtns[port-1].isChecked() ) {
			status = Relay.PORT_STATE_ON;
		}
		launchRelayToggleTask(port, status);
	}
	private void sendRelayClearMaskTask(int port) {
		Log.d(TAG, "sendRelayClearMaskTask");
		// hide ourself and clear the mask
		mainPortMaskBtns[port-1].setVisibility(View.INVISIBLE);
		launchRelayToggleTask(port, Relay.PORT_STATE_AUTO);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch ( keyCode ) {
		case KeyEvent.KEYCODE_R:
			// launch the update
			Log.d(TAG, "onKeyDown R");
			launchStatusTask();
			break;
		default:
			return super.onKeyDown(keyCode, event);
		}
		return true;
	}
	
	private void initThreading() {
		guiThread = new Handler();
		statusThread = Executors.newSingleThreadExecutor();
		controllerCommand = "";
		updateStatusScreen = true;
		updateTask = new Runnable() {
			public void run() {
				// Task to be run
				
				// Cancel any previous status check if exists
				if ( statusPending != null )
					statusPending.cancel(true);

				try {
					// Get IP & Port
					Host h = new Host();
					if ( isController() ) {
						// controller
						h.setHost(Prefs.getHost(getBaseContext()));
						h.setPort(Prefs.getPort(getBaseContext()));
					} else {
						// reeefangel.com
						h.setUserId(Prefs.getUserId(getBaseContext()));
					}
					h.setCommand(controllerCommand);
					Log.d(TAG, "Task Host: " + h.toString());
					// Create ControllerTask
					ControllerTask cTask = new ControllerTask(
							ReefAngelStatusActivity.this,
							h,
							updateStatusScreen);
					statusPending = statusThread.submit(cTask);
					// Add ControllerTask to statusThread to be run
				} catch ( RejectedExecutionException e) {
					Log.e(TAG, "initThreading RejectedExecution");
					updateTime.setText(R.string.messageError);
				}
			}
		};
	}
	
	private void launchStatusTask() {
		/**
		 * Creates the thread that communicates with the controller
		 * Then that function calls updateDisplay when it finishes
		 */
		Log.d(TAG, "launchStatusTask");
		// cancel any previous update if it hasn't started yet
		guiThread.removeCallbacks(updateTask);
		// set the command to be executed
		if ( isController() ) {
			controllerCommand = Globals.requestStatus;
		} else {
			controllerCommand = Globals.requestReefAngel;
		}
		updateStatusScreen = true;
		// start an update
		guiThread.post(updateTask);
	}
	
	private void launchRelayToggleTask(int relay, int status) {
		Log.d(TAG, "launchRelayToggleTask");
		// cancel any previous update if it hasn't started yet
		guiThread.removeCallbacks(updateTask);
		// set the command to be executed
		if ( isController() ) {
			controllerCommand = new String(String.format("%s%d%d", Globals.requestRelay, relay, status));
		} else {
			controllerCommand = Globals.requestReefAngel;
		}
		updateStatusScreen = true;
		Log.d(TAG, "RelayCommand: " + controllerCommand);
		// start an update
		guiThread.post(updateTask);
	}
	
	private boolean isController() {
		String[] devicesArray = getBaseContext().getResources().getStringArray(R.array.devicesValues);
		String device = Prefs.getDevice(getBaseContext());
		boolean b = false;
		if ( device.equals(devicesArray[0]) ) {
			b = true;
		}
		return b;
	}
	private RADbAdapter openDatabase() {
		// open the database
		Log.d(TAG, "Open database");
		RADbAdapter dbAdapter = new RADbAdapter(this);
		dbAdapter.open();
        return dbAdapter;
	}
	
	private void closeDatabase(RADbAdapter db) {
		// close the database
		Log.d(TAG, "Close database");
		db.close();
	}
	
	public void guiUpdateDisplay() {
		/**
		 * Updates the display with the values from the Controller
		 * 
		 * Called from other threads
		 */
		guiThread.post(new Runnable() {
			public void run() {
				Log.d(TAG, "updateDisplay");
				try {
					RADbAdapter dbAdapter = openDatabase();
					Cursor c = dbAdapter.getLatestParams();
					String [] values;
					short r, ron, roff;
					Relay relay = new Relay();
					
					if ( c.moveToFirst() ) {
						values = new String [] {
							c.getString(c.getColumnIndex(Globals.PCOL_LOGDATE)),
							c.getString(c.getColumnIndex(Globals.PCOL_T1)),
							c.getString(c.getColumnIndex(Globals.PCOL_T2)),
							c.getString(c.getColumnIndex(Globals.PCOL_T3)),
							c.getString(c.getColumnIndex(Globals.PCOL_PH)),
							c.getString(c.getColumnIndex(Globals.PCOL_DP)),
							c.getString(c.getColumnIndex(Globals.PCOL_AP)),
							c.getString(c.getColumnIndex(Globals.PCOL_SAL))
						};
						r = c.getShort(c.getColumnIndex(Globals.PCOL_RDATA));
						ron = c.getShort(c.getColumnIndex(Globals.PCOL_RONMASK));
						roff = c.getShort(c.getColumnIndex(Globals.PCOL_ROFFMASK));
					} else {
						values = getNeverValues();
						r = ron = roff = 0;
					}
					c.close();
					closeDatabase(dbAdapter);
					loadDisplayedControllerValues(values);
					relay.setRelayData(r, ron, roff);
					updateMainRelayValues(relay);
				} catch ( SQLException e ) {
					Log.d(TAG, "SQLException: " + e.getMessage());
				} catch ( CursorIndexOutOfBoundsException e ) {
					Log.d(TAG, "CursorIndex out of bounds: " + e.getMessage());
				}
			}
		});
	}
	
	public void guiAddParamsEntry(final Controller ra) {
		/** 
		 * Adds parameters entry to database
		 * 
		 * Called from other threads
		 */
		guiThread.post(new Runnable() {
			public void run() {
				Log.d(TAG, "addParamsEntry");
				RADbAdapter dbAdapter = openDatabase();
				dbAdapter.addParamsEntry(ra);
				closeDatabase(dbAdapter);
			}
		});
	}
	
	public void guiUpdateTimeText(final String msg) {
		/**
		 * Updates the UpdatedTime text box only
		 * 
		 * Called from other threads to indicate an error or interruption
		 */
		guiThread.post(new Runnable() {
			public void run() {
				Log.d(TAG, "updateTimeText");
				updateTime.setText(msg);
			}
		});
	}
	
	private void updateMainRelayValues(Relay r) {
		short status;
		String s;
		String s1;
		boolean useMask = isController();
		for ( int i = 0; i < 8; i++ ) {
			status = r.getPortStatus(i+1);
			if ( status == Relay.PORT_STATE_ON ) {
				s1 = "ON";
			} else if ( status == Relay.PORT_STATE_AUTO ) {
				s1 = "AUTO";
			} else {
				s1 = "OFF";
			}
			s = new String(String.format("Port %d: %s(%s)", i+1, r.isPortOn(i+1, useMask)?"ON":"OFF",s1));
			Log.d(TAG, s);
			
			mainPortBtns[i].setChecked(r.isPortOn(i+1, useMask));
			if ( ((status == Relay.PORT_ON) || (status == Relay.PORT_STATE_OFF)) && useMask ) {
				// masked on or off, show button
				mainPortMaskBtns[i].setVisibility(View.VISIBLE);
			} else {
				mainPortMaskBtns[i].setVisibility(View.INVISIBLE);
			}
		}
	}
	
	private void loadDisplayedControllerValues(String[] v) {
		// The order must match with the order in getDisplayedControllerValues
		updateTime.setText(v[0]);
		t1Text.setText(v[1]);
		t2Text.setText(v[2]);
		t3Text.setText(v[3]);
		phText.setText(v[4]);
		dpText.setText(v[5]);
		apText.setText(v[6]);
		salinityText.setText(v[7]);
	}
	
	private String[] getNeverValues() {
		return new String[] {
        		getString(R.string.messageNever),
        		getString(R.string.defaultStatusText),
        		getString(R.string.defaultStatusText),
        		getString(R.string.defaultStatusText),
        		getString(R.string.defaultStatusText),
        		getString(R.string.defaultStatusText),
        		getString(R.string.defaultStatusText),
        		getString(R.string.defaultStatusText)
        	};
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.settings:
        	// launch settings
        	Log.d(TAG, "Menu Settings clicked");
        	startActivity(new Intent(this, Prefs.class));
            break;
        case R.id.about:
        	// launch about box
        	Log.d(TAG, "Menu About clicked");
        	startActivity(new Intent(this, About.class));
            break;
        case R.id.params:
        	Log.d(TAG, "Menu Parameters clicked");
        	startActivity(new Intent(this, ParamsListActivity.class));
        	break;
        	/*
        case R.id.memory:
        	// launch memory
        	Log.d(TAG, "Memory clicked");
        	startActivity(new Intent(this, Memory.class));
        	break;
        	*/
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
}