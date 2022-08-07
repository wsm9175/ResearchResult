package com.slot.researchresult;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {
    private final String TAG = ResultAdapter.class.getSimpleName();
    private ArrayList<ResultData> mList;
    private Context context;

    public ResultAdapter(Context context) {
        this.context = context;
    }

    public void setmList(ArrayList<ResultData> mList) {
        this.mList = mList;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtNumber, txtResult, txtR, txtG, txtB;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNumber = itemView.findViewById(R.id.txt_number);
            txtResult = itemView.findViewById(R.id.txt_result);
            txtR = itemView.findViewById(R.id.txt_R);
            txtG = itemView.findViewById(R.id.txt_G);
            txtB = itemView.findViewById(R.id.txt_B);
        }
        public void onBind(ResultData resultData) {
            txtNumber.setText(resultData.getName());
            txtResult.setText(resultData.getResult());
            txtResult.setBackgroundColor(resultData.getColor());
            txtR.setText(resultData.getR());
            txtG.setText(resultData.getG());
            txtB.setText(resultData.getB());
        }
    }

    @NonNull
    @Override
    public ResultAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultAdapter.ViewHolder holder, int position) {
        holder.onBind(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }
}
