package info.curtbinder.reefangel.phone;

/*
 * Copyright (c) 2011-12 by Curt Binder (http://curtbinder.info)
 *
 * This work is made available under the terms of the 
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 * http://creativecommons.org/licenses/by-nc-sa/3.0/
 */

import info.curtbinder.reefangel.db.RAData;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ParamsListActivity extends ListActivity {

	private static final String TAG = ParamsListActivity.class.getSimpleName();
	private static final int[] TO = { R.id.plog, R.id.pt1, R.id.pt2, R.id.pt3,
	/*
	 * R.id.pph, R.id.pdp, R.id.pap, R.id.psal, R.id.patoh, R.id.patol, R.id.pr,
	 * R.id.pron, R.id.proff
	 */
	};

	private static final String[] FROM = {	RAData.PCOL_LOGDATE,
											RAData.PCOL_T1,
											RAData.PCOL_T2,
											RAData.PCOL_T3 };
	RAApplication rapp;

	public void onCreate ( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.paramslist );
		rapp = (RAApplication) getApplication();
		updateData();
	}

	private void showEvents ( Cursor cursor ) {
		Log.d( TAG, "showEvents" );
		SimpleCursorAdapter adapter =
				new SimpleCursorAdapter( this, R.layout.paramslistitem, cursor,
					FROM, TO );
		this.setListAdapter( adapter );
	}

	private void updateData ( ) {
		try {
			Cursor c = rapp.data.getAllData();
			startManagingCursor( c );
			showEvents( c );
		} catch ( SQLException e ) {
			Log.d( TAG, "SQL Exception" );
		} finally {
			// rapp.getRAData().close();
		}
	}

	public boolean onCreateOptionsMenu ( Menu menu ) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.paramslist_menu, menu );
		return true;
	}

	public boolean onOptionsItemSelected ( MenuItem item ) {
		switch ( item.getItemId() ) {
			case R.id.params_delete:
				AlertDialog.Builder builder =
						new AlertDialog.Builder( ParamsListActivity.this );
				builder.setMessage( rapp.getString( R.string.messageDeleteAllPrompt ) )
						.setCancelable( false )
						.setPositiveButton( rapp.getString( R.string.buttonYes ),
											new DialogInterface.OnClickListener() {
												public void onClick (
														DialogInterface dialog,
														int id ) {
													Log.d( TAG, "Delete all" );
													dialog.dismiss();
													rapp.data.deleteData();
													Toast.makeText( ParamsListActivity.this,
																	rapp.getString( R.string.messageDeleted ),
																	Toast.LENGTH_SHORT )
															.show();
													updateData();
												}
											} )
						.setNegativeButton( rapp.getString( R.string.buttonNo ),
											new DialogInterface.OnClickListener() {
												public void onClick (
														DialogInterface dialog,
														int id ) {
													Log.d( TAG, "Cancel Delete" );
													dialog.cancel();
												}
											} );

				AlertDialog alert = builder.create();
				alert.show();
				break;
		}
		return true;
	}

	protected void onListItemClick ( ListView l, View v, int position, long id ) {
		// super.onListItemClick( l, v, position, id );
		// Log.d(TAG, "Clicked: position:" + position + ", id:" + id);
		Intent i = new Intent( this, HistoryPopupActivity.class );
		i.putExtra( RAData.PCOL_ID, id );
		startActivity( i );
	}
}
