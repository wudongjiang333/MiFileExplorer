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

/**
 * 抽象了一个文件最基本的信息 。我自己的分析：FileInfo是存到数据库的某个文件项，FavoriteItem是收藏夹的项。
 * 不确定是path和FileInfo的filePath是否相同。
 * */
public class FileInfo {
	// 文件名
	public String fileName;
	// 文件路径
	public String filePath;
	// 文件大小（单位是啥呢？）
	public long fileSize;
	// 是否为目录
	public boolean IsDir;
	// 不懂
	public int Count;
	// 上次修改日期
	public long ModifiedDate;
	// 是否选中
	public boolean Selected;
	// 是否可读
	public boolean canRead;
	// 是否可写
	public boolean canWrite;
	// 是否隐藏
	public boolean isHidden;
	// 如果从数据库中来，就是数据库中的id
	public long dbId; // id in the database, if is from database

}
