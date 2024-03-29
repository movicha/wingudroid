package com.wingufile.wingudroid2.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wingufile.wingudroid2.ConcurrentAsyncTask;
import com.wingufile.wingudroid2.R;
import com.wingufile.wingudroid2.SeafException;


/**
 * Basic class for dialog which get input from user and carries out some
 * operation in the background.
 */
public abstract class TaskDialog extends DialogFragment {
    public static abstract class TaskDialogListener {
        public void onTaskSuccess() {
        }
        public void onTaskFailed(SeafException e) {
        }
        public void onTaskCancelled() {
        }
    }

    private static final String STATE_ERROR_TEXT = "task_dialog.error_text";
    private static final String TASK_STATE_SAVED = "task_dialog.task_saved";

    // The AsyncTask instance
    private Task task;

    // The error message
    private TextView errorText;

    // The spinning wheel to show loading
    private ProgressBar loading;

    //
    private Button okButton, cancelButton;

    // The content area of the dialog
    private View contentView;

    private TaskDialogListener mListener;

    /**
     * Create the content area of the dialog
     * @param inflater
     * @param savedInstanceState The saved dialog state. Most of the time subclasses don't need to make use of it, since the state of UI widgets is restored by the base class. 
     * @return The created view
     */
    protected abstract View createDialogContentView(LayoutInflater inflater,
                                                      Bundle savedInstanceState);
    /**
     * Create the AsyncTask
     */
    protected abstract Task prepareTask();

    /**
     * Return the content area view of the dialog
     */
    protected View getContentView() {
        return contentView;
    }

    /**
     * This hook method is called right after the dialog is built.
     * @prarm dialog
     */
    protected void onDialogCreated(Dialog dialog) {
    }

    /**
     * Save the content you are interested
     * @param outState
     */
    protected void onSaveDialogContentState(Bundle outState) {
    }

    protected Task getTask() {
        return task;
    }

    /**
     * Save the state of the background task so that we can restore the task
     * when recreating this dialog. For example, when screen rotation
     * @param outState
     */
    protected void onSaveTaskState(Bundle outState) {
    }

    /**
     * Recreate the background task when this dialog is recreated.
     * @param savedInstanceState
     * @return The background task if it should be restored. Or null to indicate that you don't want to recreate the task.
     */
    protected Task onRestoreTaskState(Bundle savedInstanceState) {
        return null;
    }

    /**
     * Check if the user input is valid. It is called when the "OK" button is
     * clicked.
     * @throws Exception with the error message if there is error in user input
     */
    protected void onValidateUserInput() throws Exception {
    }

    public void onTaskSuccess() {
        getDialog().dismiss();
        if (mListener != null) {
            mListener.onTaskSuccess();
        }
    }

    public void onTaskFailed(SeafException e) {
        hideLoading();
        showError(e.getMessage());
        enableInput();
        if (mListener != null) {
            mListener.onTaskFailed(e);
        }
    }

    public void setTaskDialogLisenter(TaskDialogListener listener) {
        mListener = listener;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        onSaveDialogContentState(outState);
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            outState.putBoolean(TASK_STATE_SAVED, true);
            onSaveTaskState(outState);
            task.cancel(true);
        }

        outState.putString(STATE_ERROR_TEXT, errorText.getText().toString());

        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        LinearLayout view = (LinearLayout)inflater.inflate(R.layout.task_dialog, null);

        contentView = createDialogContentView(inflater, savedInstanceState);
        view.addView(contentView, 0);

        errorText = (TextView)view.findViewById(R.id.error_message);
        loading = (ProgressBar)view.findViewById(R.id.loading);

        if (savedInstanceState != null) {
            String error = savedInstanceState.getString(STATE_ERROR_TEXT);
            if (error != null && error.length() > 0) {
                errorText.setText(error);
                errorText.setVisibility(View.VISIBLE);
            }
        }

        builder.setView(view);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
                    task.cancel(true);
                }
                if (mListener != null) {
                    mListener.onTaskCancelled();
                }
            }
        });

        final AlertDialog dialog = builder.create();
        final View.OnClickListener onOKButtonClickedListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    onValidateUserInput();
                } catch (Exception e) {
                    showError(e.getMessage());
                    return;
                }

                task = prepareTask();
                executeTask();
            }
        };

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                okButton.setOnClickListener(onOKButtonClickedListener);
                if (savedInstanceState != null) {
                    dialog.onRestoreInstanceState(savedInstanceState);
                    restoreTask(savedInstanceState);
                }
            }
        });

        onDialogCreated(dialog);

        return dialog;
    }

    private void restoreTask(Bundle savedInstanceState) {
        // Restore the AsyncTask and execute it
        boolean taskSaved = savedInstanceState.getBoolean(TASK_STATE_SAVED);
        if (!taskSaved) {
            return;
        }

        task = onRestoreTaskState(savedInstanceState);
        if (task != null) {
            executeTask();
        }
    }

    protected void showLoading() {
        loading.startAnimation(AnimationUtils.loadAnimation(
                                   getActivity(), android.R.anim.fade_in));
        loading.setVisibility(View.VISIBLE);
    }

    protected void hideLoading() {
        loading.startAnimation(AnimationUtils.loadAnimation(
                                   getActivity(), android.R.anim.fade_out));
        loading.setVisibility(View.INVISIBLE);
    }

    protected void showError(String error) {
        errorText.setText(error);
        errorText.startAnimation(AnimationUtils.loadAnimation(
                                   getActivity(), android.R.anim.fade_in));
        errorText.setVisibility(View.VISIBLE);
    }

    protected void hideError() {
        errorText.startAnimation(AnimationUtils.loadAnimation(
                                   getActivity(), android.R.anim.fade_out));
        errorText.setVisibility(View.GONE);
    }

    protected void disableInput() {
        okButton.setEnabled(false);
    }

    protected void enableInput() {
        okButton.setEnabled(true);
    }

    private void executeTask() {
        disableInput();
        hideError();
        showLoading();
        task.setTaskDialog(this);
        ConcurrentAsyncTask.execute(task);
    }

    public static abstract class Task extends AsyncTask<Void, Long, Void> {
        private SeafException err;
        private TaskDialog dlg;

        /**
         * Carries out the background task.
         */
        protected abstract void runTask();

        public void setTaskDialog(TaskDialog dlg) {
            this.dlg = dlg;
        }

        /**
         * Subclass should call this method to set the exception
         * @param e The exception raised during {@link runTask()}
         */
        protected void setTaskException(SeafException e) {
            err = e;
        }

        public SeafException getTaskException() {
            return err;
        }

        @Override
        public Void doInBackground(Void... params) {
            runTask();
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            if (err != null) {
                dlg.onTaskFailed(err);
            } else {
                dlg.onTaskSuccess();
            }
        }
    }
}
