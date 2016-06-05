package com.example.mumu.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {

    String httpUrl = "http://apis.baidu.com/txapi/world/world?num=12&page=1";
    String storagePath = Environment.getExternalStorageDirectory().getPath() + "/cache";
    private ArrayList<HashMap<String,Object>> listItems;//用于存放item内容
    private SimpleAdapter listItemAdapter;
    private ListView listView;
    private Button button;
    HashMap<String, String> urlMap = new HashMap<>();//存放item对应WebView的url

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        button = (Button) findViewById(R.id.refresh);
        new GetJson().execute(httpUrl);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, NewsActivity.class);
                intent.putExtra("item_url", urlMap.get("ItemUrl" + String.valueOf(position)));
                startActivity(intent);
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new GetJson().execute(httpUrl);
            }
        });
    }

    /**
     * 获取Json数据，传递给listView
     */
    public class GetJson extends AsyncTask<String, String, String> {

        BufferedReader reader = null;
        String jsonResult = null;
        StringBuffer sbf = new StringBuffer();

        /*
        开启子线程获取Json数据和下载图片
         */
        @Override
        protected String doInBackground(String... httpUrl) {
            try {
                /*
                获取json数据
                 */
                URL url = new URL(httpUrl[0]);
                HttpURLConnection connection = (HttpURLConnection) url
                        .openConnection();
                connection.setRequestMethod("GET");
                // 填入apikey到HTTP header
                connection.setRequestProperty("apikey", "68163259f28e748f8a4020d90375afec");
                connection.connect();
                InputStream is = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String strRead;
                while ((strRead = reader.readLine()) != null) {
                    sbf.append(strRead);
                    sbf.append("\r\n");
                }
                reader.close();
                jsonResult = sbf.toString();

                /*
                下载图片
                 */
                File file = new File(storagePath);
                if(!file.exists()) {file.mkdirs();}//如果储存卡内没有cache文件，创建它

                for (int i = 33; i <= 44; i++) {
                    File imageFile = new File(storagePath+"/" + String.valueOf(i)+".jpg");
                    //判断图片是否存在内部存储文件
                    if (!imageFile.exists()) {
                        String filePath = "http://s3-us-west-1.amazonaws.com/realisticshots/2016/02" + String.valueOf(i) + ".jpg";
                        URL imageUrl = new URL(filePath);
                        HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                        conn.setConnectTimeout(5 * 1000);
                        conn.setRequestMethod("GET");
                        InputStream inStream = conn.getInputStream();
                        File myCaptureFile = new File(storagePath+"/" + String.valueOf(i) + ".jpg");
                        BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
                        Bitmap mBitmap = BitmapFactory.decodeStream(inStream);
                        mBitmap.compress(Bitmap.CompressFormat.JPEG, 10, outStream);
                        outStream.flush();
                        outStream.close();
                        Toast.makeText(getApplicationContext(),
                                "Picture"+String.valueOf(i) + " has downloads in "+storagePath+"/"+ String.valueOf(i) + ".jpg",
                                Toast.LENGTH_SHORT).show();
                        Log.d("MainActivity", "File" + String.valueOf(i) + " has saved in "+storagePath+"/"+ String.valueOf(i) + ".jpg");
                    } else Log.d("MainActivity", "File" + String.valueOf(i) + " exists");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return jsonResult;
        }

        /*
        创建适配器并传递给listView
         */
        @Override
        protected void onPostExecute(String Jsonresult) {
            super.onPostExecute(Jsonresult);
            try {
                listItems = parseJson(Jsonresult);//解析Json数据
            } catch (JSONException e) {
                e.printStackTrace();
            }
            listItemAdapter = new SimpleAdapter(MainActivity.this, listItems,
                    R.layout.item,
                    new String[]{"ItemImage","ItemTitle", "ItemDescription"},
                    new int[]{R.id.imageView,R.id.titleTextView, R.id.descTextView});
            listItemAdapter.setViewBinder(new ListViewBinder());
            listView.setAdapter(listItemAdapter);
        }
    }

    /**
     * 解析Json数据的方法
     * @param jsonResult
     * @return listItems
     * @throws JSONException
     */
    public ArrayList<HashMap<String,Object>> parseJson(String jsonResult) throws JSONException{
        JSONObject demoJson = new JSONObject(jsonResult);
        JSONArray newsList = demoJson.getJSONArray("newslist");
        listItems = new ArrayList<>();
        try {
            for (int i = 0; i < newsList.length(); i++) {
                String title = newsList.getJSONObject(i).getString("title");
                String description = newsList.getJSONObject(i).getString("description");
                String url = newsList.getJSONObject(i).getString("url");
                HashMap<String, Object> map = new HashMap<>();
                map.put("ItemImage", getBitmap(i+33));
                map.put("ItemTitle", title);
                map.put("ItemDescription", description);
                urlMap.put("ItemUrl"+String.valueOf(i), url);
                listItems.add(map);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return listItems;
    }

    /*
    把bitmap格式图片放到imageview
     */
    private class ListViewBinder implements ViewBinder{

        @Override
        public boolean setViewValue(View view,Object data,String textRepresentation){
            if((view instanceof ImageView)&&(data instanceof Bitmap)){
                ImageView imageView = (ImageView) view;
                Bitmap bmp = (Bitmap) data;
                imageView.setImageBitmap(bmp);
                return true;
            }
            return false;
        }
    }

    //从储存卡中获取items的图片
    public Bitmap getBitmap(int i){
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(storagePath+"/"+String.valueOf(i)+".jpg", opts);
        opts.inSampleSize = computeSampleSize(opts, -1, 128*128);
        opts.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(storagePath+"/"+String.valueOf(i)+".jpg", opts);

        return bitmap;
    }

    /*
    计算缩放比例
     */
    public static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels){
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        int roundedSize = 1;
        if(initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    /*
    计算缩放比例
     */
    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength ,int maxNumOfPixels){
        double w = options.outWidth;
        double h= options.outHeight;

        int lowerBound = (maxNumOfPixels == -1) ? 1 :
                (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 :
                (int) Math.floor(h / minSideLength);

        if(upperBound < lowerBound) {
            return lowerBound;
        }
        if((maxNumOfPixels == -1 && minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }
}
