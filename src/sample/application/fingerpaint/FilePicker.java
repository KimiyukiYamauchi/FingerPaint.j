package sample.application.fingerpaint;

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class FilePicker extends ListActivity {

	String dir, externalStorageDir;
	FileFilter fFilter;
	Comparator<Object> comparator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filelist);
		externalStorageDir = Environment.getExternalStorageDirectory()
				.toString();
		SharedPreferences pref = this.getSharedPreferences("FilePickerPrefs",
				MODE_PRIVATE);
		dir = pref.getString("Folder", externalStorageDir + "/mypaint");
		makeFileFilter();
		makeComparator();
		showList();
	}

	@Override
	protected void onStop() {
		super.onStop();
		SharedPreferences pref = getSharedPreferences("FilePickerPrefs",
				MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString("Folder", dir);
		editor.commit();
	}

	private void showList() {
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			finish();
		}
		File file = new File(dir);
		if (!file.exists()) {
			dir = externalStorageDir;
			file = new File(dir);
		}
		this.setTitle(dir);
		File[] fc = file.listFiles(fFilter);
		final FileListAdapter adapter = new FileListAdapter(this, fc);
		adapter.sort(comparator);
		ListView lv = (ListView) findViewById(android.R.id.list);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				if (((File) adapter.getItem(position)).isDirectory()) {
					dir = ((File) adapter.getItem(position)).getPath();
					showList();
				} else {
					Intent i = new Intent();
					i.putExtra("fn",
							((File) adapter.getItem(position)).getPath());
					setResult(RESULT_OK, i);
					finish();
				}
			}
		});
	}

	private void makeComparator() {
		comparator = new Comparator<Object>() {

			@Override
			public int compare(Object object1, Object object2) {
				int pad1 = 0;
				int pad2 = 0;
				File file1 = (File) object1;
				File file2 = (File) object2;
				if (file1.isDirectory()) {
					pad1 = -65536;
				}
				if (file2.isDirectory()) {
					pad1 = -65536;
				}
				return pad1 - pad2
						+ file1.getName().compareToIgnoreCase(file2.getName());
			}
		};
	}

	private void makeFileFilter() {
		fFilter = new FileFilter() {

			@Override
			public boolean accept(File file) {
				Pattern p = Pattern.compile(
						"\\.png$|\\.jpg$|\\.gif$|\\.jpeg$|\\.bmp$",
						Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(file.getName());
				boolean shown = (m.find() || file.isDirectory())
						&& !file.isHidden();
				return shown;
			}
		};
	}

	public void upButtonClick(View v) {
		dir = new File(dir).getParent();
		showList();
	}

}
