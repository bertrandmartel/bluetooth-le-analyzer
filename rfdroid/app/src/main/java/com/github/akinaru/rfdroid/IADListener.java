package com.github.akinaru.rfdroid;

import java.util.List;

/**
 * Created by akinaru on 05/02/16.
 */
public interface IADListener {

    public void onADframeReceived(long ts, List<Long> history);

}
