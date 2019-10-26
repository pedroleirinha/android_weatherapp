package com.thunder.coolweatherbro;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;

public class MainActivity extends AppCompatActivity {

    private enum requestType {
        weather,
        forecast
    }

    private ColumnChartView chart;
    private ColumnChartData data;
    private SearchView citySearched;

    private String APIKEY = "9f56df1c095f61ff1204cf06c4e767ee";
    private String weatherAPIPrefixURL = "http://api.openweathermap.org/data/2.5/";
    private RequestQueue queue;
    private SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queue = Volley.newRequestQueue(this);

        citySearched = findViewById(R.id.searchView);

        mAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, null, new String[]{"cityName"}, new int[]{android.R.id.text1}, CursorAdapter.IGNORE_ITEM_VIEW_TYPE);

        citySearched.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                makeRequestByCityName(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                makeRequestsCities(newText.replace(" ", "%20"));
                return false;
            }
        });

        citySearched.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor cursor = (Cursor) mAdapter.getItem(position);
                String txt = cursor.getString(cursor.getColumnIndex("cityName"));
                citySearched.setQuery(txt, true);
                return true;
            }
        });

        citySearched.setSuggestionsAdapter(mAdapter);

        setupDrawerAndToolbar();


    }

    @Override
    protected void onStart() {
        super.onStart();
        createChart();
        LocationClass.getInstance(this).getCurrentLocation();
        requestCurrentLocationWeather();
    }

    // You must implements your logic to get data using OrmLite
    private void populateAdapter(String[] array) {
        final MatrixCursor c = new MatrixCursor(new String[]{BaseColumns._ID, "cityName"});
        for (int i = 0; i < array.length; i++) {
            c.addRow(new Object[]{i, array[i]});
        }
        mAdapter.changeCursor(c);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    public void createChart() {
        chart = findViewById(R.id.timeLineChart);

        List<AxisValue> axisValues = new ArrayList<>();
        List<Column> columns = new ArrayList<>();

        for (int i = 0; i < 8; ++i) {

            SubcolumnValue value = new SubcolumnValue(0, ContextCompat.getColor(this, R.color.colorPrimary));
            axisValues.add(new AxisValue(i));
            Column column = new Column(Collections.singletonList(value));
            column.setHasLabels(true);
            columns.add(column);
        }

        data = new ColumnChartData(columns);

        data.setAxisXBottom(new Axis(axisValues).setHasLines(true).setTextColor(getColor(android.R.color.black)));
        data.setAxisYLeft(Axis.generateAxisFromRange(0, 50, 2).setHasLines(true).setMaxLabelChars(2).setTextColor(getColor(android.R.color.black)));

        chart.setColumnChartData(data);

        // Set selection mode to keep selected month column highlighted.
        chart.setValueSelectionEnabled(true);

        chart.setZoomType(ZoomType.HORIZONTAL);
    }

    public void updateChart(WeatherForecast info) {

        List<AxisValue> axisValues = data.getAxisXBottom().getValues();

        for (int i = 0; i < 8; ++i) {
            Column column = data.getColumns().get(i);

            String date = info.list[i].dt_txt.substring(info.list[i].dt_txt.length() - 8, info.list[i].dt_txt.length() - 3);
            float currentTemp = (float) info.list[i].main.temp;

            SubcolumnValue value = new SubcolumnValue(currentTemp, ContextCompat.getColor(this, R.color.colorPrimary));

            axisValues.get(i).setLabel(date);
            column.setValues(Collections.singletonList(value));
        }

        chart.startDataAnimation();
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_sync:
                requestCurrentLocationWeather();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void makeRequestByCityName(String cityName) {
        String params = "q=" + cityName + "&appid=" + APIKEY + "&units=metric";
        makeWeatherRequests(requestType.weather, params);
        makeWeatherRequests(requestType.forecast, params);
    }

    public void makeWeatherRequests(final requestType request, String params) {

        String localURL = weatherAPIPrefixURL + request.toString() + "?" + params;

        Log.d("Request", localURL);
        final ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("How's the weather here..?");
        progressDialog.show();

        StringRequest weatherRequest = new StringRequest(Request.Method.GET, localURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response);
                        if (request == requestType.weather) {
                            showWeatherInfo(new Gson().fromJson(response, WeatherObject.class));
                        } else {
                            updateChart(new Gson().fromJson(response, WeatherForecast.class));
                        }
                        progressDialog.dismiss();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Response Error", "Error:" + error);
                progressDialog.dismiss();
            }
        });

        // Add the request to the RequestQueue.
        queue.add(weatherRequest);
    }

    public void makeRequestsCities(String cityName) {
        Log.d("Request", cityName);

        String url = "http://geodb-free-service.wirefreethought.com/v1/geo/cities?limit=5&offset=0&sort=-population&namePrefix=" + cityName;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.d("Response", response);
                        CityObjects temp = new Gson().fromJson(response, CityObjects.class);

                        String[] cities = new String[temp.data.length];
                        for (int i = 0; i < temp.data.length; i++) {
                            Log.d("City", temp.data[i].toString());
                            cities[i] = temp.data[i].toString();
                        }
                        populateAdapter(cities);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Response Error", "Error");
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public void showWeatherInfo(WeatherObject weatherInfo) {
        TextView weatherTemp = findViewById(R.id.weatherCurrentTemp),
                weatherCityName = findViewById(R.id.weatherCityName),
                weatherCountry = findViewById(R.id.weatherCountryInitials),
                weatherLowestTemp = findViewById(R.id.weatherMinTemp),
                weatherHighestTemp = findViewById(R.id.weatherMaxTemp),
                weatherDescription = findViewById(R.id.weatherDescription),
                weatherHumidity = findViewById(R.id.weatherHumidity),
                weatherClouds = findViewById(R.id.weatherClouds),
                weatherWindSpeed = findViewById(R.id.weatherWindSpeed),
                weatherWindDirection = findViewById(R.id.weatherWindDirection),
                weatherCoordLat = findViewById(R.id.weatherLat),
                weatherCoordLon = findViewById(R.id.weatherLon),
                weatherSunRise = findViewById(R.id.weatherSunRise),
                weatherSunSet = findViewById(R.id.weatherSunSet),
                weatherTimeZone = findViewById(R.id.weatherTimeZone);


        weatherTemp.setText(String.format(Locale.UK, "%.1f º", weatherInfo.main.temp));
        weatherCityName.setText(weatherInfo.name);
        weatherCountry.setText(weatherInfo.sys.country);
        weatherHighestTemp.setText(String.format(Locale.UK, "%.1f º", weatherInfo.main.temp_max));
        weatherLowestTemp.setText(String.format(Locale.UK, "%.1f º", weatherInfo.main.temp_min));
        weatherDescription.setText(String.format(Locale.UK, "%s -> %s", weatherInfo.weather[0].main, weatherInfo.weather[0].description));
        weatherHumidity.setText(String.format(Locale.UK, "%d %%", weatherInfo.main.humidity));
        weatherClouds.setText(String.format(Locale.UK, "%d %%", weatherInfo.clouds.all));
        weatherWindSpeed.setText(String.format(Locale.UK, "%.1f m/s", weatherInfo.wind.speed));
        Log.d("Wind Deg", String.valueOf(Math.round(weatherInfo.wind.deg)));
        weatherWindDirection.setText(String.format(Locale.UK, "%d º", Math.round(weatherInfo.wind.deg)));
        weatherCoordLat.setText(String.valueOf(weatherInfo.coord.lat));
        weatherCoordLon.setText(String.valueOf(weatherInfo.coord.lon));

        SimpleDateFormat sdt = new SimpleDateFormat("HH:mm", new Locale("PT"));

        String pickedTimeZone = findTimeZone(weatherInfo.sys.country, weatherInfo.name);


        Calendar sunRise = Calendar.getInstance(TimeZone.getTimeZone(pickedTimeZone));
        sunRise.setTimeZone(TimeZone.getTimeZone(weatherInfo.name + "/" + weatherInfo.sys.country));
        sunRise.setTimeInMillis(weatherInfo.sys.sunrise * 1000L);
        weatherSunRise.setText(sdt.format(sunRise.getTime()));

        Calendar sunSet = Calendar.getInstance(TimeZone.getTimeZone(pickedTimeZone));
        sunSet.setTimeInMillis(weatherInfo.sys.sunset * 1000L);
        weatherSunSet.setText(sdt.format(sunSet.getTime()));

        weatherTimeZone.setText(pickedTimeZone);


        new DownloadImageTask((ImageView) findViewById(R.id.weatherIcon)).execute("http://openweathermap.org/img/w/" + weatherInfo.weather[0].icon + ".png");
    }

    public String findTimeZone(String countryCode, String cityName) {
        String[] availableTimeZones = android.icu.util.TimeZone.getAvailableIDs(countryCode);

        for (String timeZone : availableTimeZones) {
            if (timeZone.contains(cityName)) {
                return timeZone;
            }
        }

        return availableTimeZones[0];
    }

    public void requestCurrentLocationWeather() {
        String params = "lat=" + LocationClass.getInstance(this).getLatFromCurrentLocation() + "&lon=" + LocationClass.getInstance(this).getLongFromCurrentLocation() + "&appid=" + APIKEY + "&units=metric";
        makeWeatherRequests(requestType.weather, params);
        makeWeatherRequests(requestType.forecast, params);
    }


    @Override
    protected void onStop() {
        super.onStop();
        LocationClass.getInstance(this).destroyLocationService();
    }

    public void setupDrawerAndToolbar() {

        Toolbar toolbar;

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Cool Weather Bruh");
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout;
        ActionBarDrawerToggle toggle;
        NavigationView navigationView;

        drawerLayout = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return true;
            }
        });
    }
}

class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    private ImageView bmImage;

    DownloadImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        bmImage.setImageBitmap(result);
    }
}

