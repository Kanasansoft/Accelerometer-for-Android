package com.kanasansoft.android.Accelerometer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;

public class Main extends Activity implements SensorEventListener, Serializable {

	private static final long serialVersionUID = 1L;

	ArrayList<SensorData> data = new ArrayList<SensorData>();

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		setContentView(new AccelerometerView(this));

	}

	@Override
	protected void onResume() {

		super.onResume();

		SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

		Sensor sensor = sensors.get(0);
		sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);

	}

	@Override
	protected void onPause() {

		super.onPause();

		SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		sensorManager.unregisterListener(this);

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {

		super.onRestoreInstanceState(savedInstanceState);

		if(savedInstanceState!=null){
			data=(ArrayList<SensorData>) savedInstanceState.getSerializable("data");
		}

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {

		super.onSaveInstanceState(outState);

		if(data!=null){
			outState.putSerializable("data", data);
		}

	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {

		synchronized (data) {
			data.add(new SensorData(event));
			while(data.size() != 0){
				if(data.get(0).timestamp + 10000000000L < event.timestamp){
					data.remove(0);
				}else{
					break;
				}
			}
		}

	}

	public class SensorData implements Serializable {
		private static final long serialVersionUID = 1L;
		public int accuracy;
//		Sensor sensor;
		public long timestamp;
		public float[] values;
		public long timeMillis;
		public SensorData(){}
		public SensorData(SensorEvent event){
			accuracy = event.accuracy;
//			sensor = event.sensor;
			timestamp = event.timestamp;
			values = event.values.clone();
			timeMillis = System.currentTimeMillis();
		}
	}

	class AccelerometerView extends SurfaceView implements Callback, Runnable {

		private Thread thread;
		private int[] draw = {3,0,1,2};
		private int[] colors = {Color.RED,Color.GREEN,Color.BLUE,Color.WHITE};
		private int[] sizes = {1,1,1,2};
		private String[] labels = {"x","y","z","acc"};

		public AccelerometerView(Context context) {

			super(context);

			getHolder().addCallback(this);

			thread = new Thread(this);
			thread.start();

		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		}

		public void surfaceCreated(SurfaceHolder holder) {
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			thread = null;
		}

		public void run() {

			while(thread!=null){

				draw();

				try {
					Thread.sleep(30);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}

		}

		private void draw(){

			Canvas canvas = null;
			SurfaceHolder holder = getHolder();
			try {
				canvas = holder.lockCanvas();

				if(canvas==null){
					return;
				}

				canvas.drawRGB(0, 0, 0);

				float height = (float)canvas.getHeight();
				float width = (float)canvas.getWidth();

				Paint paint = new Paint();
				paint.setAntiAlias(true);

				paint.setColor(Color.GRAY);
				paint.setStrokeWidth(1);
				canvas.drawLine(0, height*1F/4F, width, height*1F/4F, paint);
				canvas.drawLine(0, height*2F/4F, width, height*2F/4F, paint);
				canvas.drawLine(0, height*3F/4F, width, height*3F/4F, paint);

				long currentTimeMillis = System.currentTimeMillis();
				float[][] lines=new float[4][];
				synchronized (data) {
					for(int i=0;i<4;i++){
						lines[i]= new float[data.size()*4];
					}
					for(int i=0;i<data.size();i++){
						SensorData datum = data.get(i);
						float xx = width-(currentTimeMillis-datum.timeMillis)*width/10000L;
						for(int j=0;j<3;j++){
							float yy = (-datum.values[j]*height/4F/10F+height/2F);
							lines[j][i*4+0]=lines[j][i*4+2]=xx;
							lines[j][i*4+1]=lines[j][i*4+3]=yy;
							lines[3][i*4+1]+=Math.pow(datum.values[j],2.0);
						}
						lines[3][i*4+0]=lines[3][i*4+2]=xx;
						lines[3][i*4+1]=lines[3][i*4+3]=(float)(-Math.pow(lines[3][i*4+1],0.5)*height/4F/10F+height/2F);
					}
					if(data.size()>0){
						paint.setTextSize(14);
						float[] lastValues = data.get(data.size()-1).values;
						double acc = 0;
						for(int i=0;i<3;i++){
							paint.setColor(colors[i]);
							canvas.drawText(labels[i]+":"+lastValues[i], 10, i*14+16, paint);
							acc += Math.pow(lastValues[i],2.0);
						}
						paint.setColor(colors[3]);
						canvas.drawText(labels[3]+":"+Math.pow(acc,0.5), 10, 3*14+16, paint);
						if(data.size()>=2){
							long nano = data.get(data.size()-1).timestamp-data.get(data.size()-2).timestamp;
							paint.setColor(colors[3]);
							canvas.drawText("interval(ms):"+(nano/1000000F), 10, 4*14+16, paint);
						}
					}
				}
				for(int i=0;i<4;i++){
					int l=draw[i];
					paint.setColor(colors[l]);
					paint.setStrokeWidth(sizes[l]);
					canvas.drawLines(lines[l], 2, lines[l].length-4, paint);
				}
			} catch(ArrayIndexOutOfBoundsException e) {
			} finally {
				if(canvas!=null){
					getHolder().unlockCanvasAndPost(canvas);
				}
			}

		}

	}

}
