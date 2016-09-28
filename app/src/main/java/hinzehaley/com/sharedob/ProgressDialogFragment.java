package hinzehaley.com.sharedob;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Created by haleyhinze on 9/28/16.
 * DialogFragment that shows progress spinner. Created as a DialogFragment
 * so that the instance is not lost on screen rotation.
 */
public class ProgressDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Connecting to device...");
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        progressDialog.setCancelable(false);
        return progressDialog;
    }
}

