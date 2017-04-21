/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * This file is part of FileExplorer.
 *
 * FileExplorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileExplorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.micode.fileexplorer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

/**
 * Java应用程序中、Android、Windows开发、Linux Shell都会有文件File的概念，他们本质是一样的。
 * 文件的核心概念基本一致，都是用的操作系统的文件概念，不同操作系统之间的区别也不大。 创建、删除、重命名、复制、粘贴，输入-执行-输出，也都一样。
 * 文件操作工具类，执行文件的创建、移动、粘贴、重命名、删除等
 * 
 * @author uidq0303
 * 
 */
public class FileOperationHelper {
	private static final String LOG_TAG = "FileOperation";
	// 内部文件集合，用来临时保存复制、移动等操作，用户选择的文件集合
	private ArrayList<FileInfo> mCurFileNameList = new ArrayList<FileInfo>();

	private boolean mMoving;

	private IOperationProgressListener mOperationListener;

	private FilenameFilter mFilter;
	/**
	 * 监听文件变化的接口
	 * @author uidq0303
	 *
	 */
	public interface IOperationProgressListener {
		void onFinish();

		void onFileChanged(String path);
	}

	public FileOperationHelper(IOperationProgressListener l) {
		mOperationListener = l;
	}

	public void setFilenameFilter(FilenameFilter f) {
		mFilter = f;
	}

	// 根据路径和文件名，创建文件
	public boolean CreateFolder(String path, String name) {
		Log.v(LOG_TAG, "CreateFolder >>> " + path + "," + name);

		File f = new File(Util.makePath(path, name));
		if (f.exists())
			return false;

		return f.mkdir();
	}

	// 拷贝若干个文件，把文件集合拷贝到“当前文件集合中mCurFileNameList”，可以供“粘贴操作”使用
	public void Copy(ArrayList<FileInfo> files) {
		copyFileList(files);
	}

	// 粘贴，把当前文件集合中“mCurFileNameList”的文件，拷贝到目标路径下
	public boolean Paste(String path) {
		if (mCurFileNameList.size() == 0)
			return false;

		final String _path = path;
		// 异步执行某个任务
		asnycExecute(new Runnable() {
			@Override
			public void run() {
				for (FileInfo f : mCurFileNameList) {
					CopyFile(f, _path);
				}
				// 通知操作变化
				mOperationListener.onFileChanged(Environment
						.getExternalStorageDirectory().getAbsolutePath());
				// 粘贴之后，需要清空mCurFileNameList
				clear();
			}
		});

		return true;
	}

	// 是否可以“粘贴”，mCurFileNameList有元素
	public boolean canPaste() {
		return mCurFileNameList.size() != 0;
	}

	// 开始移动，标记“正在移动”，拷贝文件集合
	public void StartMove(ArrayList<FileInfo> files) {
		if (mMoving)
			return;

		mMoving = true;
		copyFileList(files);
	}

	// 移动状态
	public boolean isMoveState() {
		return mMoving;
	}

	// 能否移动，假设path为“C:/a/b”,f.filePath为“C:、/a/b/c/d.png”,不能移动
	// TODO 感觉不太靠谱啊，为啥不能移动到文件的上级目录呢？
	public boolean canMove(String path) {
		for (FileInfo f : mCurFileNameList) {
			if (!f.IsDir)
				continue;

			if (Util.containsPath(f.filePath, path))
				return false;
		}

		return true;
	}

	// 清空当前文件集合
	public void clear() {
		synchronized (mCurFileNameList) {
			mCurFileNameList.clear();
		}
	}

	// 停止移动，移动文件是异步执行，结束后有事件通知
	public boolean EndMove(String path) {
		if (!mMoving)
			return false;
		mMoving = false;

		if (TextUtils.isEmpty(path))
			return false;

		final String _path = path;
		asnycExecute(new Runnable() {
			@Override
			public void run() {
				for (FileInfo f : mCurFileNameList) {
					MoveFile(f, _path);
				}

				mOperationListener.onFileChanged(Environment
						.getExternalStorageDirectory().getAbsolutePath());

				clear();
			}
		});

		return true;
	}

	public ArrayList<FileInfo> getFileList() {
		return mCurFileNameList;
	}

	// 异步执行某个任务
	// android的类AsyncTask对线程间通讯进行了包装，提供了简易的编程方式来使后台线程和UI线程进行通讯：后台线程执行异步任务，并把操作结果通知UI线程。
	// 可以参考http://blog.csdn.net/xufenghappy6/article/details/7343899
	private void asnycExecute(Runnable r) {
		final Runnable _r = r;
		new AsyncTask() {
			@Override
			protected Object doInBackground(Object... params) {
				synchronized (mCurFileNameList) {
					_r.run();
				}
				if (mOperationListener != null) {
					mOperationListener.onFinish();
				}

				return null;
			}
		}.execute();
	}

	// 某个路径是否被选中
	public boolean isFileSelected(String path) {
		synchronized (mCurFileNameList) {
			for (FileInfo f : mCurFileNameList) {
				if (f.filePath.equalsIgnoreCase(path))
					return true;
			}
		}
		return false;
	}

	// 文件重命名
	public boolean Rename(FileInfo f, String newName) {
		if (f == null || newName == null) {
			Log.e(LOG_TAG, "Rename: null parameter");
			return false;
		}

		File file = new File(f.filePath);
		String newPath = Util.makePath(Util.getPathFromFilepath(f.filePath),
				newName);
		final boolean needScan = file.isFile();
		try {
			boolean ret = file.renameTo(new File(newPath));
			if (ret) {
				if (needScan) {
					mOperationListener.onFileChanged(f.filePath);
				}
				mOperationListener.onFileChanged(newPath);
			}
			return ret;
		} catch (SecurityException e) {
			Log.e(LOG_TAG, "Fail to rename file," + e.toString());
		}
		return false;
	}

	// 删除若干文件，先copy文件集合，再异步执行删除操作，删除完成后，有通知
	public boolean Delete(ArrayList<FileInfo> files) {
		copyFileList(files);
		asnycExecute(new Runnable() {
			@Override
			public void run() {
				for (FileInfo f : mCurFileNameList) {
					DeleteFile(f);
				}

				mOperationListener.onFileChanged(Environment
						.getExternalStorageDirectory().getAbsolutePath());

				clear();
			}
		});
		return true;
	}

	// 删除1个文件
	protected void DeleteFile(FileInfo f) {
		if (f == null) {
			Log.e(LOG_TAG, "DeleteFile: null parameter");
			return;
		}

		File file = new File(f.filePath);
		boolean directory = file.isDirectory();
		if (directory) {
			for (File child : file.listFiles(mFilter)) {
				if (Util.isNormalFile(child.getAbsolutePath())) {
					DeleteFile(Util.GetFileInfo(child, mFilter, true));
				}
			}
		}

		file.delete();

		Log.v(LOG_TAG, "DeleteFile >>> " + f.filePath);
	}

	// 执行1个文件的拷贝，如果文件是目录，拷贝整个目录，可能有递归Copy
	private void CopyFile(FileInfo f, String dest) {
		if (f == null || dest == null) {
			Log.e(LOG_TAG, "CopyFile: null parameter");
			return;
		}

		File file = new File(f.filePath);
		if (file.isDirectory()) {

			// directory exists in destination, rename it
			String destPath = Util.makePath(dest, f.fileName);
			File destFile = new File(destPath);
			int i = 1;
			while (destFile.exists()) {
				destPath = Util.makePath(dest, f.fileName + " " + i++);
				destFile = new File(destPath);
			}

			for (File child : file.listFiles(mFilter)) {
				if (!child.isHidden()
						&& Util.isNormalFile(child.getAbsolutePath())) {
					CopyFile(Util.GetFileInfo(child, mFilter, Settings
							.instance().getShowDotAndHiddenFiles()), destPath);
				}
			}
		} else {
			String destFile = Util.copyFile(f.filePath, dest);
		}
		Log.v(LOG_TAG, "CopyFile >>> " + f.filePath + "," + dest);
	}

	// 移动文件，通过重命名的方式，移动的
	private boolean MoveFile(FileInfo f, String dest) {
		Log.v(LOG_TAG, "MoveFile >>> " + f.filePath + "," + dest);

		if (f == null || dest == null) {
			Log.e(LOG_TAG, "CopyFile: null parameter");
			return false;
		}

		File file = new File(f.filePath);
		String newPath = Util.makePath(dest, f.fileName);
		try {
			return file.renameTo(new File(newPath));
		} catch (SecurityException e) {
			Log.e(LOG_TAG, "Fail to move file," + e.toString());
		}
		return false;
	}

	// 把文件集合copy到mCurFileNameList中，同步~
	private void copyFileList(ArrayList<FileInfo> files) {
		synchronized (mCurFileNameList) {
			mCurFileNameList.clear();
			for (FileInfo f : files) {
				mCurFileNameList.add(f);
			}
		}
	}

}
