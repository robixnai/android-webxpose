/*
 * Copyright (C) 2011 The Code Bakers
 * Authors: Cleuton Sampaio e Francisco Rodrigues
 * e-mail: thecodebakers@gmail.com
 * Project: http://http://code.google.com/p/tcbnewswidget
 * Site: http://www.thecodebakers.org
 *
 * Licensed under the GNU GPL, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://gplv3.fsf.org/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Cleuton Sampaio e Francisco Rogrigues - thecodebakers@gmail.com
 */
package org.thecodebakers.webxposepro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;


public class WsFirstTime extends Activity {
	
	private CheckBox agree;
	private WsFirstTime selfRef;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.firsttime);
        selfRef = this;
	}
	
	public void fechar(View view) {
		Resources res = this.getResources();
		agree = (CheckBox) this.findViewById(R.id.chkOK);
		if (agree.isChecked()) {
			SharedPreferences prefs = getSharedPreferences(res.getString(R.string.WXPROprefs), MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.clear();
			editor.putBoolean("first_time",false);
			if (editor.commit()) {
				this.setResult(RESULT_OK);
			}
			else {
				this.setResult(RESULT_CANCELED);
    	        new AlertDialog.Builder(this).setMessage(res.getString(R.string.errorWritingPrefs))
    	        .setNeutralButton(res.getString(R.string.voltar), new DialogInterface.OnClickListener() {
    	            public void onClick(DialogInterface dialog, int which) {
    	            	selfRef.finish();
    	            } }).show(); 				
			}
		}
		else {
			this.setResult(RESULT_CANCELED);
		}
		this.finish();
	}
	
}
