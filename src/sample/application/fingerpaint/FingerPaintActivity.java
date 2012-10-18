package sample.application.fingerpaint;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;

public class FingerPaintActivity extends Activity implements OnTouchListener {

	Canvas canvas;
	Paint paint;
	Path path;
	Bitmap bitmap;
	float x1, y1;
	int w, h;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		ImageView iv = (ImageView) findViewById(R.id.imageView1);
		Display disp = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		w = disp.getWidth();
		h = disp.getHeight();
		bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		paint = new Paint();
		path = new Path();
		canvas = new Canvas(bitmap);

		paint.setStrokeWidth(5);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		canvas.drawColor(Color.WHITE);
		iv.setImageBitmap(bitmap);
		iv.setOnTouchListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_save:
			save();
			break;
		case R.id.menu_open:
			Intent intent = new Intent(this, FilePicker.class);
			startActivityForResult(intent, 0);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		float x = event.getX();
		float y = event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			path.reset();
			path.moveTo(x, y);
			x1 = x;
			y1 = y;
			break;
		case MotionEvent.ACTION_MOVE:
			path.quadTo(x1, y1, x, y);
			x1 = x;
			y1 = y;
			canvas.drawPath(path, paint);
			path.reset();
			path.moveTo(x, y);
			break;
		case MotionEvent.ACTION_UP:
			if (x == x1 && y == y1) {
				y1 = y1 + 1;
			}
			path.quadTo(x1, y1, x, y);
			canvas.drawPath(path, paint);
			path.reset();
			break;
		}
		ImageView iv = (ImageView) this.findViewById(R.id.imageView1);
		iv.setImageBitmap(bitmap);

		return true;
	}

	void save() {

		SharedPreferences prefs = getSharedPreferences(
				"FingerPaintPreferences", MODE_PRIVATE);
		int imageNumber = prefs.getInt("imageNumber", 1);
		File file = null;

		if (externalMediaChecker()) {
			DecimalFormat form = new DecimalFormat("0000");
			String path = Environment.getExternalStorageDirectory()
					+ "/mypaint/";
			File outDir = new File(path);
			if (!outDir.exists()) {
				outDir.mkdir();
			}

			do {
				file = new File(path + "img" + form.format(imageNumber)
						+ ".png");
				imageNumber++;
			} while (file.exists());
			if (writeImage(file)) {
				scanMedia(file.getPath());
				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt("imageNumber", imageNumber);
				editor.commit();
			}
		}
	}

	private boolean writeImage(File file) {
		FileOutputStream fo;
		try {
			fo = new FileOutputStream(file);
			bitmap.compress(CompressFormat.PNG, 100, fo);
			fo.flush();
			fo.close();
		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
			return false;
		}
		return true;
	}

	private boolean externalMediaChecker() {
		boolean result = false;
		String status = Environment.getExternalStorageState();
		if (status.equals(Environment.MEDIA_MOUNTED)) {
			result = true;
		}
		return result;
	}

	MediaScannerConnection mc;

	void scanMedia(final String fp) {
		mc = new MediaScannerConnection(this,
				new MediaScannerConnection.MediaScannerConnectionClient() {

					@Override
					public void onScanCompleted(String path, Uri uri) {
						disconnect();
					}

					private void disconnect() {
						mc.disconnect();
					}

					@Override
					public void onMediaScannerConnected() {
						scanFile(fp);
					}

					private void scanFile(String fp) {
						mc.scanFile(fp, "image/png");
					}
				});
		mc.connect();
	}

	Bitmap loadImage(String path) {
		boolean landscape = false;
		Bitmap bm;

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);
		int oh = options.outHeight;
		int ow = options.outWidth;

		if (ow > oh) {
			landscape = true;
			oh = options.outWidth;
			ow = options.outHeight;
		}

		options.inJustDecodeBounds = false;
		options.inSampleSize = Math.max(ow / w, oh / h);
		bm = BitmapFactory.decodeFile(path, options);

		if (landscape) {
			Matrix matrix = new Matrix();
			matrix.setRotate(90.0f);
			bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(),
					matrix, false);
		}

		bm = Bitmap.createScaledBitmap(bm, (int) (w),
				(int) (w * (((double) oh) / ((double) ow))), false);
		Bitmap offBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas offCanvas = new Canvas(offBitmap);
		offCanvas.drawBitmap(bm, 0, (h - bm.getHeight()) / 2, null);
		bm = offBitmap;
		return bm;

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(getLocalClassName(), "onActivityResult resultCode = "
				+ resultCode);
		if (resultCode == RESULT_OK) {
			Log.d(getLocalClassName(), "equestCode == RESULT_OK ");
			bitmap = loadImage(data.getStringExtra("fn"));
			canvas = new Canvas(bitmap);
			ImageView iv = (ImageView) this.findViewById(R.id.imageView1);
			iv.setImageBitmap(bitmap);
		}
	}

}
