package com.alvin.CustomViewLoadImage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.LruCache;
import android.widget.ImageView;

public class ImageDownloadHelper {
	public Map<String, SoftReference<Bitmap>> softCaches = new LinkedHashMap<String, SoftReference<Bitmap>>();
	public LruCache<String, Bitmap> lruCache = null;

	public interface OnImageDownloadListener {
		void onImageDownload(Bitmap bitmap, String imgUrl);
	}

	public ImageDownloadHelper() {
		int memoryAmount = (int) Runtime.getRuntime().maxMemory();
		// 获取剩余内存的8分之一作为缓存
		int cacheSize = memoryAmount / 8;
		if (lruCache == null) {
			lruCache = new MyLruCache(cacheSize);
		}
	}

	// 异步加载图片方法
	public void myDownloadImage(Context context, String url,
			ImageView imageView, OnImageDownloadListener downloadListener) {
		Bitmap bitmap = null;
		// 先从强引用中拿数据
		if (lruCache != null) {
			bitmap = lruCache.get(url);
		}
		if (bitmap != null && url.equals(imageView.getTag())) {
			// Log.i(TAG, "==从强引用中找到数据" + bitmap.toString());
			imageView.setImageBitmap(bitmap);
		} else {
			SoftReference<Bitmap> softReference = softCaches.get(url);
			if (softReference != null) {
				bitmap = softReference.get();
			}
			// 从软引用中拿数据
			if (bitmap != null && url.equals(imageView.getTag())) {
				// Log.i(TAG, "==从软引用中找到数据" + bitmap.toString());
				imageView.setImageBitmap(bitmap);

				// 添加到强引用中
				lruCache.put(url, bitmap);
				// 从软引用集合中移除
				softCaches.remove(url);
			} else {
				// 从文件缓存中拿数据
				if (url != null) {
//					String imageName = getImageName(url);
					byte[] imgNameBytes = md5(url.getBytes());//  利用 md5 算法 获取唯一的 图片名字
					String imageName = toHex(imgNameBytes)+".png";;
					String cachePath = SDCardHelper
							.getInstance().getSDCardCachePath(context);
					String imgDir = cachePath + File.separator + imageName;
					bitmap = SDCardHelper.getInstance()
							.loadBitmapFromSDCard(imgDir);
					if (bitmap != null && url.equals(imageView.getTag())) {
						// Log.i(TAG, "==从文件缓存中找到数据" + bitmap.toString());
						imageView.setImageBitmap(bitmap);
						// 放入强缓存
						lruCache.put(url, bitmap);
					} else {
						new MyAsyncTask(context, url, imageView,
								downloadListener,imgDir).execute(url);
					}
				}
			}
		}
	}
	public static Bitmap createThumbnail(String filePath,int sampleSize) {
		// 第一次采样，目的是采集图片的边界
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
//		BitmapFactory.decodeFile(filePath, options);
//		int oldHeight = options.outHeight;
//		int oldWidth = options.outWidth;
//		// 第二次采样，必须要获取缩略之后的新图片的每个像素的信息
//		int ratioWidth = oldWidth / width;
//		int ratioHeight = oldHeight / height;
//
//		options.inSampleSize = ratioWidth > ratioHeight ? ratioWidth
//				: ratioHeight;
		options.inSampleSize = sampleSize;//  缩放 1/(sampleSize*sampleSize)
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		options.inJustDecodeBounds = false;
		Bitmap bm = BitmapFactory.decodeFile(filePath, options);
		return bm;
	}
	// 异步任务类
	class MyAsyncTask extends AsyncTask<String, Void, Bitmap> {
		public Context context;
		public ImageView mImageView;
		public String url;
		public OnImageDownloadListener downloadListener;

		private String imgPath;

		public MyAsyncTask(Context context, String url, ImageView mImageView,
				OnImageDownloadListener downloadListener,String imgPath) {
			this.context = context;
			this.url = url;
			this.mImageView = mImageView;
			this.downloadListener = downloadListener;
			this.imgPath = imgPath;

		}

		@Override
		protected Bitmap doInBackground(String... params) {
			Bitmap bm = null;
			try {
				String urlString = params[0];
				URL urlObj = new URL(urlString);
				HttpURLConnection httpConn = (HttpURLConnection) urlObj
						.openConnection();
				httpConn.setDoInput(true);
				httpConn.setRequestMethod("GET");
				httpConn.connect();
				if (httpConn.getResponseCode() == 200) {
					InputStream is = httpConn.getInputStream();
					bm = BitmapFactory.decodeStream(is);
				}
				if (bm != null) {
					byte[] imgNameBytes = md5(url.getBytes());//  利用 md5 算法 获取唯一的 图片名字
					String imageName = toHex(imgNameBytes)+".png";
					boolean flag = SDCardHelper
							.getInstance().saveBitmapToSDCardPrivateCacheDir(
									bm, imageName, context);
					if (flag) {
						// Log.i(TAG, "==从网络中找到数据" + bm.toString());
						// 放入强缓存
						bm = createThumbnail(imgPath,5);//  放入请引用前二次采样
						lruCache.put(urlString, bm);
					} else {
						// removeTaskFromMap(urlString);
					}
					return bm;
				} else {
					// removeTaskFromMap(urlString);
				}
			} catch (Exception e) {
				e.printStackTrace();
				// removeTaskFromMap(urlString);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			// 回调设置图片
			if (downloadListener != null && result != null) {
				downloadListener.onImageDownload(result, url);
			}
		}
	}

	// 强引用缓存类
	class MyLruCache extends LruCache<String, Bitmap> {
		public MyLruCache(int maxSize) {
			super(maxSize);
		}
		@Override
		protected int sizeOf(String key, Bitmap value) {
			return value.getHeight() * value.getWidth() * 4;
			// Bitmap图片的一个像素是4个字节
			// return value.getRowBytes() * value.getHeight();
		}

		@Override
		protected void entryRemoved(boolean evicted, String key,
				Bitmap oldValue, Bitmap newValue) {
			if (evicted) {
				SoftReference<Bitmap> softReference = new SoftReference<Bitmap>(
						oldValue);
				softCaches.put(key, softReference);
			}
		}
	}

	// SDCard工具类
	static class SDCardHelper {
		public static SDCardHelper sdCardHelper;

		public static SDCardHelper getInstance() {
			if (sdCardHelper == null) {
				sdCardHelper = new SDCardHelper();
			}
			return sdCardHelper;
		}

		// 判断SDCard是否挂载
		public boolean isSDCardMounted() {
			return Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED);
		}

		// 获取SDCard的根目录路径
		public String getSDCardBasePath() {
			if (isSDCardMounted()) {
				return Environment.getExternalStorageDirectory()
						.getAbsolutePath();
			} else {
				return null;
			}
		}

		// 获取SDCard的完整空间大小
		public long getSDCardTotalSize() {
			long size = 0;
			if (isSDCardMounted()) {
				StatFs statFs = new StatFs(getSDCardBasePath());
				if (Build.VERSION.SDK_INT >= 18) {
					size = statFs.getTotalBytes();
				} else {
					size = statFs.getBlockCount() * statFs.getBlockSize();
				}
				return size / 1024 / 1024;
			} else {
				return 0;
			}
		}

		// 获取SDCard的可用空间大小
		public long getSDCardAvailableSize() {
			long size = 0;
			if (isSDCardMounted()) {
				StatFs statFs = new StatFs(getSDCardBasePath());
				if (Build.VERSION.SDK_INT >= 18) {
					size = statFs.getAvailableBytes();
				} else {
					size = statFs.getAvailableBlocks() * statFs.getBlockSize();
				}
				return size / 1024 / 1024;
			} else {
				return 0;
			}
		}

		// 获取SDCard的剩余空间大小
		public long getSDCardFreeSize() {
			long size = 0;
			if (isSDCardMounted()) {
				StatFs statFs = new StatFs(getSDCardBasePath());
				if (Build.VERSION.SDK_INT >= 18) {
					size = statFs.getFreeBytes();
				} else {
					size = statFs.getFreeBlocks() * statFs.getBlockSize();
				}
				return size / 1024 / 1024;
			} else {
				return 0;
			}
		}

		// 保存byte[]文件到SDCard的指定公有目录
		public boolean saveFileToSDCardPublicDir(byte[] data, String type,
				String fileName) {
			if (isSDCardMounted()) {
				BufferedOutputStream bos = null;
				File file = Environment.getExternalStoragePublicDirectory(type);

				try {
					bos = new BufferedOutputStream(new FileOutputStream(
							new File(file, fileName)));
					bos.write(data);
					bos.flush();
					return true;
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				} finally {
					if (bos != null) {
						try {
							bos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} else {
				return false;
			}
		}

		// 保存byte[]文件到SDCard的自定义目录
		public boolean saveFileToSDCardCustomDir(byte[] data, String dir,
				String fileName) {
			if (isSDCardMounted()) {
				BufferedOutputStream bos = null;
				File file = new File(getSDCardBasePath() + File.separator + dir);
				if (!file.exists()) {
					file.mkdirs();// 递归创建子目录
				}
				try {
					bos = new BufferedOutputStream(new FileOutputStream(
							new File(file, fileName)));
					bos.write(data, 0, data.length);
					bos.flush();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (bos != null) {
						try {
							bos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				return true;
			} else {
				return false;
			}
		}

		// 保存byte[]文件到SDCard的指定私有Files目录
		public boolean saveFileToSDCardpublicDir(byte[] data, String type,
				String fileName, Context context) {
			if (isSDCardMounted()) {
				BufferedOutputStream bos = null;
				// 获取私有Files目录
				File file = context.getExternalFilesDir(type);
				try {
					bos = new BufferedOutputStream(new FileOutputStream(
							new File(file, fileName)));
					bos.write(data, 0, data.length);
					bos.flush();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (bos != null) {
						try {
							bos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				return true;
			} else {
				return false;
			}
		}

		// 保存byte[]文件到SDCard的私有Cache目录
		public boolean saveFileToSDCardpublicCacheDir(byte[] data,
				String fileName, Context context) {
			if (isSDCardMounted()) {
				BufferedOutputStream bos = null;
				// 获取私有的Cache缓存目录
				File file = context.getExternalCacheDir();
				// Log.i("SDCardHelper", "==" + file);
				try {
					bos = new BufferedOutputStream(new FileOutputStream(
							new File(file, fileName)));
					bos.write(data, 0, data.length);
					bos.flush();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (bos != null) {
						try {
							bos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				return true;
			} else {
				return false;
			}
		}

		// 保存bitmap图片到SDCard的私有Cache目录
		public boolean saveBitmapToSDCardPrivateCacheDir(Bitmap bitmap,
				String fileName, Context context) {
			if (isSDCardMounted()) {
				BufferedOutputStream bos = null;
				// 获取私有的Cache缓存目录
				File file = context.getExternalCacheDir();
				try {
					bos = new BufferedOutputStream(new FileOutputStream(
							new File(file, fileName)));
					if (fileName != null
							&& (fileName.contains(".png") || fileName
									.contains(".PNG"))) {
						bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
					} else {
						bitmap = createThumbnail(fileName,5);
						bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
					}
					bos.flush();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (bos != null) {
						try {
							bos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				return true;
			} else {
				return false;
			}
		}

		// 从SDCard中寻找指定目录下的文件，返回byte[]
		public byte[] loadFileFromSDCard(String filePath) {
			BufferedInputStream bis = null;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			File file = new File(filePath);
			if (file.exists()) {
				try {
					bis = new BufferedInputStream(new FileInputStream(file));
					byte[] buffer = new byte[1024 * 8];
					int c = 0;
					while ((c = (bis.read(buffer))) != -1) {
						baos.write(buffer, 0, c);
						baos.flush();
					}
					return baos.toByteArray();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (bis != null) {
						try {
							bis.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (baos != null) {
						try {
							baos.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			return null;
		}

		// 从SDCard中寻找指定目录下的文件，返回Bitmap
		public Bitmap loadBitmapFromSDCard(String filePath) {
			byte[] data = loadFileFromSDCard(filePath);
			if (data != null) {
				Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
				if (bm != null) {
					//进行二次采样
//					bm = createThumbnail(filePath, 100,
//							100);
					return bm;
				}
			}
			return null;
		}

		// 获取SDCard私有的Cache目录
		public String getSDCardCachePath(Context context) {
			return context.getExternalCacheDir().getAbsolutePath();
		}

		// 获取SDCard私有的Files目录
		public String getSDCardFilePath(Context context, String type) {
			return context.getExternalFilesDir(type).getAbsolutePath();
		}

		// 从sdcard中删除文件
		public boolean removeFileFromSDCard(String filePath) {
			File file = new File(filePath);
			if (file.exists()) {
				try {
					file.delete();
					return true;
				} catch (Exception e) {
					return false;
				}
			} else {
				return false;
			}
		}
	}

	public String getImageName(String url) {
		String imageName = "";
		if (url != null) {
			imageName = url.substring(url.lastIndexOf("/") + 1);
		}
		return imageName;
	}
	/**
	 *   MD5 消息摘要 对输入的内容，进行唯一标识的计算，
	 *   16个字节，32个字符，sha1更长
	 *   只是   单向的
	 *   应用
	 *   图片缓存 ，图片网址进行 MD5 计算，结合toHex()方法,结果作为图片名字
	 * @param data
	 * @return
	 */
	public byte[] md5(byte[] data){
		byte[] ret = null;
		if (data != null) {
			try {
				//  创建 消息摘要 对象
				MessageDigest digest = MessageDigest.getInstance("MD5");
				ret = digest.digest(data);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}
	/**
	 *   字节数组转 16 进制字符串，用于 传递 字节数组信息
	 * @param data
	 * @return
	 */
	public static String toHex(byte[] data){
		String ret = null;
		if (data != null) {
			int len = data.length;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < len; i++) {
				int iv = data[i];
				//  取高四位
				int ih = (iv>>4) & 0x0F;
				//  取低四位
				int il = iv & 0x0F;
				char ch,cl;
				if(ih>9){
					ch = (char)('A' + (ih -10));
				}else{
					ch = (char)('0' + ih);
				}
				if(il>9){
					cl = (char)('A' + (il -10));
				}else{
					cl = (char)('0' + il);
				}
				sb.append(ch).append(cl);
			}
			ret = sb.toString();
		}
		return ret;
	}
}
