package com.example.mumu.myapplication;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {

    String httpUrl = "http://apis.baidu.com/txapi/world/world?num=12&page=1";
    private ArrayList<HashMap<String,Object>> listItems;//用于存放item内容
    private SimpleAdapter listItemAdapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        new GetJson().execute(httpUrl);
    }


    public class GetJson extends AsyncTask<String, String, String> {

        BufferedReader reader = null;
        String jsonResult = null;
        StringBuffer sbf = new StringBuffer();

        /*
        开启子线程获取Json数据
         */
        @Override
        protected String doInBackground(String... httpUrl) {
            try {
                URL url = new URL(httpUrl[0]);
                HttpURLConnection connection = (HttpURLConnection) url
                        .openConnection();
                connection.setRequestMethod("GET");
                // 填入apikey到HTTP header
                connection.setRequestProperty("apikey", "68163259f28e748f8a4020d90375afec");
                connection.connect();
                InputStream is = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String strRead = null;
                while ((strRead = reader.readLine()) != null) {
                    sbf.append(strRead);
                    sbf.append("\r\n");
                }
                reader.close();
                jsonResult = sbf.toString();
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
                listItems = parseJson(Jsonresult);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            listItemAdapter = new SimpleAdapter(MainActivity.this, listItems,
                    R.layout.item,
                    new String[]{"ItemImage","ItemTitle", "ItemDescription"},
                    new int[]{R.id.imageView,R.id.titleTextView, R.id.descTextView});
            listView.setAdapter(listItemAdapter);
        }
    }

    /**
     * 解析Json数据的方法
     * @param jsonResult
     * @return ArrayList<HashMap<String,Object>>
     * @throws JSONException
     */
    public ArrayList<HashMap<String,Object>> parseJson(String jsonResult) throws JSONException{
        JSONObject demoJson = new JSONObject(jsonResult);
        JSONArray newsList = demoJson.getJSONArray("newslist");
        listItems = new ArrayList<HashMap<String, Object>>();

        try {
            for (int i = 0; i < newsList.length(); i++) {
                String title = newsList.getJSONObject(i).getString("title");
                String description = newsList.getJSONObject(i).getString("description");
                /*String url = newsList.getJSONObject(i).getString("url");*/
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("ItemImage", R.drawable.i0233);
                map.put("ItemTitle", title);
                map.put("ItemDescription", description);
                /*map.put("ItemUrl", url);*/
                listItems.add(map);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return listItems;
    }

}
