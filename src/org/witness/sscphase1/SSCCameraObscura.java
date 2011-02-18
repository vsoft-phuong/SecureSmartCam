package com.lvh.SSCCameraObscura;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class SSCCameraObscura extends Activity implements 
	OnGestureListener, OnLongClickListener, OnClickListener, OnTouchListener, SurfaceHolder.Callback {
    
	private static final String SSC = "[SSCCameraObscura] ****************************";
	Vibrator vibe;
	MenuInflater mi;
	Canvas canvas;
	Paint paint;
	
	Uri imgResource;
	Drawable img;
	int imgId; 
	
	int viewState = 1; // locked view by default
	RelativeLayout lockedView, editView;
	View editViewHolder;
	
	float cox,coy;
	int ROIs = 0;
	int[] buttonIDs;
	
	SSCCalculator calc;
	Bitmap yellowTag, greyTag, move;
	
	boolean isEditing;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN );
        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        calc = new SSCCalculator();
        
        yellowTag = BitmapFactory.decodeResource(getResources(), R.drawable.uitagbackgroundyellow);
        greyTag = BitmapFactory.decodeResource(getResources(), R.drawable.uitagbackgroundgrey);
        move = BitmapFactory.decodeResource(getResources(), R.drawable.uimove);
        
        /*
         *  obviously, user will pass resource ID or chosen image
         *  and it will be set here.
         *  for our demo-ing purposes, we will suppose an image
         *  and declare it to begin with.
         *  
         *  I'm using R-values here, but in the real app,
         *  the drawable should be passed as a URI
         */ 
        imgResource = Uri.parse("");
        img = this.getResources().getDrawable(R.drawable.mainimage);
        
        /*
         * also, each image should have an ID, probably generated by our 
         * secure media store.  For all intents and purposes, i'm calling this 
         * image "1" 
         */
        imgId = 1;
        
        setContentView(R.layout.cameraobscura);
    	lockedView = (RelativeLayout) findViewById(R.id.lockedView);
        lockedView.setOnTouchListener(this);
        lockedView.setOnLongClickListener(this);
        lockedView.setBackgroundDrawable(img);
        
        setThisView(viewState);
    }
    
    public void setThisView(int s) {
        // to start, there are 2 views: one locked view for adding tags, and one "editable" for editing tags.
        // this is only really relevant should the user come back to the app and state hasn't been preserved.
        switch(s) {
        case 1:
        	// locked view
        	uiUpdateMainView();
        	lockedView.setClickable(true);
        	isEditing = false;
        	break;
        case 2:        	
        	// 1. TODO: grey-out the ImageView children and make them un-clickable
        	for(int x=0;x<lockedView.getChildCount();x++) {
        		lockedView.getChildAt(x).setClickable(false);
        	}
        	// 2. TODO: make the image itself un-clickable.  This doesn't work: why?
        	lockedView.setClickable(false);
        	break;
        }
    }
    
    public void uiNewTag(float x, float y) {
    	vibe.vibrate(50);
    	//viewState = 2;
    	//setThisView(viewState);
    	
    	SSCTagContainer tc = new SSCTagContainer(cox,coy);
    	// 1. set a new tag with default dimensions around the point of click
    	/*
    	ImageView movePoint = new ImageView(this);
    	RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(75,75);
    	movePoint.setBackgroundDrawable(new BitmapDrawable(move));
    	movePoint.setContentDescription("movepoint");
    	movePoint.setOnClickListener(this);
    	
    	lp.leftMargin = (int) cox;
    	lp.topMargin = (int) coy;
    	lockedView.addView(movePoint,lp);
    	
    	isEditing = true;
    	*/
    	
    	// TODO: when we come back from the edit view, sate the old state back
    	//viewState = 1;
    	//setThisView(viewState);
    	isEditing = false;
    	uiRegisterNewTag(tc.initNewTag());
    }
    
    public void uiEditTag(CharSequence vCoords) {
    	vibe.vibrate(50);
    	viewState = 1;
    	SSCEditTag et = new SSCEditTag(vCoords,lockedView);
    	buttonIDs = et.getButtonIDs();
    	OnClickListener ocl = new OnClickListener() {
			public void onClick(View v) {
				if(v.getId() == buttonIDs[0]) {
					// Edit Tag
					Log.v(SSC,"Edit Tag clicked");
				} else if(v.getId() == buttonIDs[1]) {
					// ID Tag
					Log.v(SSC,"ID Tag clicked");
				} else if(v.getId() == buttonIDs[2]) {
					// Blur Tag
					Log.v(SSC,"Blur Tag clicked");
				} else if(v.getId() == buttonIDs[3]) {
					// Image Prefs
					Log.v(SSC,"Image Prefs clicked");
					launchImagePrefs();
				}
			}
    	};
    	et.addActions(ocl);
    	et.show();
    }
    
    public void uiRegisterNewTag(float[] newTagCoords) {
    	/*
    	 * this method adds the returned coordinates to our array of ROIs
    	 * and creates a JSON String for identifying it permanently
    	 */
    	String newTagCoordsDescription = "{\"id\":" + ROIs + ",\"coords\":[";
    	for(int x=0;x<4;x++) {
    		newTagCoordsDescription += Float.toString(newTagCoords[x]) + ",";
    	}
    	newTagCoordsDescription = newTagCoordsDescription.substring(0,newTagCoordsDescription.length() - 1) + "]}";
    	ROIs++;
    	Log.v(SSC,"New tag created with parameters [" + newTagCoordsDescription + "]");
    	uiUpdateMainView(newTagCoordsDescription);
    }
    
    public void uiUpdateMainView(String newTagCoordsDescription) {
    	/*
    	 *  when we switch back to locked mode,
    	 *  we will have to update the number of ImageViews
    	 *  on the screen according to the ROI array
    	 */
    	
    	// 1. unpack the coordinates from the passed string
    	float[] coords = new float[4];
    	try {
			coords = calc.jsonDeflate(1,newTagCoordsDescription);
		} catch (Exception e) {
			Log.d(SSC,"JSON ERROR MOTHERFUCK: " + e);
		}
		
		int[] dimensions = calc.getTagDimensions(coords);
		
		ImageView i = new ImageView(this);
    	Bitmap reB = Bitmap.createScaledBitmap(yellowTag,dimensions[0],dimensions[1], true);
    	i.setImageBitmap(reB);
    	
    	RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(dimensions[0],dimensions[1]);
    	lp.leftMargin = (int) coords[0];
    	lp.topMargin = (int) coords[3];
    	i.setAdjustViewBounds(false);
    	i.setContentDescription(newTagCoordsDescription);
    	i.setOnClickListener(this);
    	
    	// LASTLY!
    	lockedView.addView(i,lp);

    	
    	// TODO: iterate through other ROIs according to their content description?
    	// not sure if i will have to do this-- maybe when we return from the edit screen
    	// this view will remain intact (i doubt it!)
    }
    
    public void uiUpdateMainView() {
    	Log.v(SSC,"NUMBER OF ROIs = " + ROIs);
    }
    
    public void launchImagePrefs() {
    	Intent i = new Intent(this,SSCImagePrefs.class);
    	i.putExtra("imgId", imgId);
    	startActivityForResult(i,0);
    }
    
	public boolean onLongClick(View v) {
		if(v == lockedView) {
			uiNewTag(cox,coy);
		}
		return false;
	}

	public void onClick(View v) {
		String tagType = (String) v.getContentDescription();
		if(tagType.compareTo("movepoint") == 0) {
			Log.d(SSC,"touching the movepoint");
		} else {
			uiEditTag(v.getContentDescription());
		}
			
	}

	public boolean onTouch(View v, MotionEvent e) {
			cox = e.getX();
			coy = e.getY();
			if(v.getContentDescription() != null) {
				Log.d(SSC,v.getContentDescription().toString());
			}
		return false;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	public void onLongPress(MotionEvent e) {}
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		
		return false;
	}
	public void onShowPress(MotionEvent e) {}
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}
	public boolean onDown(MotionEvent e) {
		return false;
	}
	
	public void makeToast(String t) {
		Toast.makeText(this, t, Toast.LENGTH_LONG);
	}
	
	  public boolean onCreateOptionsMenu(Menu m) {
	    	mi = getMenuInflater();
	    	mi.inflate(R.menu.menu, m);
			return true;
	    }
	    
	    public boolean onOptionsItemSelected(MenuItem item) {
	    	switch(item.getItemId()) {
	    	case R.id.menuUndo:
	    		return true;
	    	case R.id.menuNewTag:
	    		return true;
	    	case R.id.menuSave:
	    		return true;
	    	case R.id.menuImagePrefs:
	    		launchImagePrefs();
	    		return true;
	    	case R.id.menuShare:
	    		return true;
	    	case R.id.menuMore:
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
	    	}
	    }
	    
    
    protected void onConfigurationChanged() {
    	// I might need to handle this, but i set against in in the manifest for the main activity, so maybe not.
    	Log.d(SSC,"Orientation changed: ROIs = " + ROIs);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.d(SSC,"RESUMING app: ROIs = " + ROIs);
    }

    @Override
    protected void onPause() {
    	super.onPause();
    	Log.d(SSC,"PAUSING app: ROIs = " + ROIs);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	Log.d(SSC,"STOPPING app: ROIs = " + ROIs);
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	Log.d(SSC,"DESTROYED app: ROIs = " + ROIs);
    }

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

	public void surfaceCreated(SurfaceHolder holder) {
		
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		
	}
}