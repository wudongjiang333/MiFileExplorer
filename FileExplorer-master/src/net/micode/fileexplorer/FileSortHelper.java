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

import java.util.Comparator;
import java.util.HashMap;

/**
 * 文件排序工具类 .
 * 可以按照文件的名字、大小、日期、类型，排序 .
 * 调用示例：Collections.sort(List<FileInfo>,Comparator).
 * 
 * 
 * 
 * FileSortHelper的核心功能就是，对文件集合FileInfo排序。 FileInfo有若干字段，根据字段定义了4种比较器Comparator。
 * 调用示例：Collections.sort(List<FileInfo>, Comparator);
 * 实现排序，FileInfo可以实现Comparable接口，但是比较方式是固定的，也就是说只能采用一种方式排序。
 * 而Comparator则比较灵活，更像是一种“策略模式”，传入不同的“策略”，实现不同方式的排序。
 * 
 * @author uidq0303
 * 
 */
public class FileSortHelper {

	public enum SortMethod {
		name, size, date, type
	}
	
	// 排序类型  
	private SortMethod mSort;
	
	// 是否文件优先
	private boolean mFileFirst;
	
	// 比较器，这个才是关键,实现compare方法  
	private HashMap<SortMethod, Comparator> mComparatorList = new HashMap<SortMethod, Comparator>();

	public FileSortHelper() {
		mSort = SortMethod.name;
		mComparatorList.put(SortMethod.name, cmpName);
		mComparatorList.put(SortMethod.size, cmpSize);
		mComparatorList.put(SortMethod.date, cmpDate);
		mComparatorList.put(SortMethod.type, cmpType);
	}

	public void setSortMethog(SortMethod s) {
		mSort = s;
	}

	public SortMethod getSortMethod() {
		return mSort;
	}

	public void setFileFirst(boolean f) {
		mFileFirst = f;
	}

	public Comparator getComparator() {
		return mComparatorList.get(mSort);
	}

	private abstract class FileComparator implements Comparator<FileInfo> {

		@Override
		public int compare(FileInfo object1, FileInfo object2) {
			if (object1.IsDir == object2.IsDir) {
				return doCompare(object1, object2);
			}

			if (mFileFirst) {
				// the files are listed before the dirs
				return (object1.IsDir ? 1 : -1);
			} else {
				// the dir-s are listed before the files
				return object1.IsDir ? -1 : 1;
			}
		}

		protected abstract int doCompare(FileInfo object1, FileInfo object2);
	}

	private Comparator cmpName = new FileComparator() {
		@Override
		public int doCompare(FileInfo object1, FileInfo object2) {
			return object1.fileName.compareToIgnoreCase(object2.fileName);
		}
	};

	private Comparator cmpSize = new FileComparator() {
		@Override
		public int doCompare(FileInfo object1, FileInfo object2) {
			return longToCompareInt(object1.fileSize - object2.fileSize);
		}
	};

	private Comparator cmpDate = new FileComparator() {
		@Override
		public int doCompare(FileInfo object1, FileInfo object2) {
			return longToCompareInt(object2.ModifiedDate - object1.ModifiedDate);
		}
	};

	private int longToCompareInt(long result) {
		return result > 0 ? 1 : (result < 0 ? -1 : 0);
	}

	private Comparator cmpType = new FileComparator() {
		@Override
		public int doCompare(FileInfo object1, FileInfo object2) {
			int result = Util.getExtFromFilename(object1.fileName)
					.compareToIgnoreCase(
							Util.getExtFromFilename(object2.fileName));
			if (result != 0)
				return result;

			return Util.getNameFromFilename(object1.fileName)
					.compareToIgnoreCase(
							Util.getNameFromFilename(object2.fileName));
		}
	};
}
