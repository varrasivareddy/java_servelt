package nilzor.httpperformance.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.future.ResponseFuture;
import com.squareup.otto.Subscribe;
import nilzor.httpperformance.R;
import nilzor.httpperformance.ServiceLocator;
import nilzor.httpperformance.core.Action;
import nilzor.httpperformance.core.OttoGsonRequest;
import nilzor.httpperformance.entities.HttpBinGetResponse;
import nilzor.httpperformance.entities.TestServiceResponse;
import nilzor.httpperformance.messages.VolleyRequestSuccess;
import nilzor.httpperformance.viewmodels.VolleyRequestActivityViewModel;

import java.util.ArrayList;

public class MainActivity extends Activity {
    //private final String Url = "http://httpbin.org/get";
    //private final String Url = "http://httpbin.org/delay/1";
    private final String Url = "http://5.150.231.5:80";
    private VolleyRequestActivityViewModel _model;
    private static final String TAG = "OVDR";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.main);
        ServiceLocator.ensureInitialized(this);
        _model = new VolleyRequestActivityViewModel();
        Ion.getDefault(this).configure().setLogging("ION", Log.DEBUG);
        Ion.getDefault(this).configure();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        if(_model.listenForResponse) {
            ServiceLocator.ResponseBuffer.startSaving();
            ServiceLocator.EventBus.unregister(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if (_model.listenForResponse) {
            ServiceLocator.EventBus.register(this);
            ServiceLocator.ResponseBuffer.stopAndProcess();
        }
        bindUi();
        // Add change listener to the switch. Must be done after binding, since this might trigger unregister
        ((Switch)findViewById(R.id.eventListenSwitch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onListenForResponseChanged(isChecked);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("Model", _model);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState()");
        _model = (VolleyRequestActivityViewModel) savedInstanceState.getSerializable("Model");
    }

    private void bindUi() {
        ((TextView) findViewById(R.id.statusText)).setText(_model.status);
        ((TextView) findViewById(R.id.prevResultText)).setText(_model.prevResult);
        ((Switch) findViewById(R.id.eventListenSwitch)).setChecked(_model.listenForResponse);
    }

    /*******************************************************/

    public void onPerformHttpClicked(final View view) {

    }

    private ArrayList<RequestToken> mRequests;

    public void onIonGsonSpew(final View view) {
        spewRequests(new Action() {
            @Override
            public void act() {
                performIonGsonHttpAsync();
            }
        });
    }

    public void onIonStringSpew(final View view) {
        spewRequests(new Action() {
            @Override
            public void act() {
                performIonStringAsync();
            }
        });
    }

    private static final int SPEW_COUNT = 80;

    private void spewRequests(Action action) {
        mRequests = new ArrayList<RequestToken>(SPEW_COUNT);
        for (int i = 0; i < SPEW_COUNT; i++) {
            action.act();
        }
    }

    private void onRequestDone(RequestToken req) {
        mRequests.add(req);
        if (mRequests.size() == SPEW_COUNT) {
            onAllRequestsDone();
        }
    }

    private void onAllRequestsDone() {
        long totalTime = getMaxRequestTime();
        Log.d(TAG, String.format("Total time: %s", totalTime));
    }

    private long getMaxRequestTime() {;
        long max = 0;
        for (RequestToken token : mRequests) {
            max = Math.max(token.getDuration(), max);
        }
        return max;
    }

    private void performIonGsonHttpAsync() {
        final RequestToken req = new RequestToken();
        ResponseFuture<TestServiceResponse> future = Ion.with(this).load(Url).as(TestServiceResponse.class);
        req.onStart();
        future.setCallback(new FutureCallback<TestServiceResponse>() {
            @Override
            public void onCompleted(Exception e, TestServiceResponse testServiceResponse) {
                req.onDone();
                Log.d(TAG, String.format("GsonReq %02d finished in %04d ms", req.id, req.getDuration()));
                onRequestDone(req);
            }
        });
        Log.d(TAG, String.format("GsonReq %02d started", req.id));
    }

    private void performIonStringAsync() {
        final RequestToken req = new RequestToken();
        ResponseFuture<String> future = Ion.with(this).load(Url).asString();
        req.onStart();
        future.setCallback(new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String testServiceResponse) {
                req.onDone();
                Log.d(TAG, String.format("StringReq %02d finished in %04d ms", req.id, req.getDuration()));
                onRequestDone(req);
            }
        });
        Log.d(TAG, String.format("StringReq %02d started", req.id));
    }

    private static class RequestToken {
        private static int mIdCounter = 0;
        public int id = ++mIdCounter;
        private long startTime;
        private long endTime;

        public long getDuration() {
            return endTime - startTime;
        }

        public void onStart() {
            startTime = System.currentTimeMillis();
        }

        public void onDone() {
            endTime = System.currentTimeMillis();
        }
    }

    public void onListenForResponseChanged(boolean isChecked) {
        if (isChecked != _model.listenForResponse){
            _model.listenForResponse = isChecked;
            registerServiceBus(_model.listenForResponse);
            Log.d(TAG, "Listen for response: " + _model.listenForResponse);
        }
    }

    private void registerServiceBus(boolean register) {
        if (register) {
            ServiceLocator.EventBus.register(this);
            ServiceLocator.ResponseBuffer.stopAndProcess();
        }
        else {
            ServiceLocator.ResponseBuffer.startSaving();
            ServiceLocator.EventBus.unregister(this);
        }
    }

    @Subscribe
    public void onHttpResponseReceived(VolleyRequestSuccess<HttpBinGetResponse> message) {
        Log.d(TAG, "Request end: " + message.requestId);
        updateUiForResponseReceived(message);
    }
    private void updateUiForRequestSent(OttoGsonRequest<HttpBinGetResponse> request) {
        _model.status = "Sent #" + request.requestId;
        bindUi();
    }

    private void updateUiForResponseReceived(VolleyRequestSuccess<HttpBinGetResponse> message) {
        _model.status = "Received #" + message.requestId;
        _model.prevResult = "#" + message.requestId + " -- " + message.response.headers.X_Request_Id;
        bindUi();
    }
}
