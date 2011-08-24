package org.thecodebakers.webxposepro;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class WsControllerHelp extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ajuda);
    }
    
    public void fechar(View view) {
    	this.finish();
    }
    
    public void web(View view) {
        Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.thecodebakers.org"));
        startActivity(viewIntent); 
    }
    
    public void wiki(View view) {
        Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://code.google.com/p/android-webxpose/w/list"));
        startActivity(viewIntent);    	
    }
}
