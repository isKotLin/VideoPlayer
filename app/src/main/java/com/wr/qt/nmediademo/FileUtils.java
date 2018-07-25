package com.wr.qt.nmediademo;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class FileUtils {


    public static List<MovieMo> getSpecificTypeOfFile(Context context, String[] extension)
    {
        //从外存中获取
        List<MovieMo>movieList=new ArrayList<>();
        Uri fileUri= MediaStore.Files.getContentUri("external");
//        Uri fileUri=Uri.parse(Environment.getExternalStorageDirectory().toString());
        Log.e("path", MediaStore.Files.getContentUri("external").toString()+"---"+MediaStore.Files.getContentUri("external/usbhost1"));
        //筛选列，这里只筛选了：文件路径和不含后缀的文件名
        String[] projection=new String[]{
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
        };
        //构造筛选语句
        String selection="";
        for(int i=0;i<extension.length;i++)
        {
            if(i!=0)
            {
                selection=selection+" OR ";
            }
            selection=selection+ MediaStore.Files.FileColumns.DATA+" LIKE '%"+extension[i]+"'";
        }
        //按时间递增顺序对结果进行排序;待会从后往前移动游标就可实现时间递减
        String sortOrder= MediaStore.Files.FileColumns.DATE_MODIFIED;
        //获取内容解析器对象
        ContentResolver resolver=context.getContentResolver();
        //获取游标
        Cursor cursor=resolver.query(fileUri, projection, selection, null, sortOrder);
        if(cursor==null){
            return movieList;
        }

        //游标从最后开始往前递减，以此实现时间递减顺序（最近访问的文件，优先显示）
        if(cursor.moveToLast())
        {
            //int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            do{
                //输出文件的完整路径
                String data=cursor.getString(2);
                MovieMo movie = new MovieMo();
                movie.size = cursor.getString(0);
                movie.name = cursor.getString(1);
//                int duration = cursor.getInt(durationCol);
//                movie.setDuration(duration);
                movie.duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                movie.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                movieList.add(movie);
                Log.d("tag", data+movie.getName()+"---"+movie.name);
            }while(cursor.moveToPrevious());
            return movieList;
        }
        cursor.close();
        return null;
    }
}