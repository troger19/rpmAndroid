package com.newventuresoftware.waveformdemo;


import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.alespero.expandablecardview.ExpandableCardView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TrainingAdapter extends RecyclerView.Adapter<TrainingAdapter.TrainingViewHolder> {

    private ArrayList<TrainingDto> dataList;

    TrainingAdapter(ArrayList<TrainingDto> dataList) {
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public TrainingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.row_training, parent, false);
        return new TrainingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrainingViewHolder holder, int position) {
        holder.txtDuration.setText(String.valueOf(dataList.get(position).getDuration()));
        holder.txtAverage.setText(String.valueOf(dataList.get(position).getAverage()));
        holder.card.setTitle(trimTrainingDate(dataList.get(position).getDate()));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    class TrainingViewHolder extends RecyclerView.ViewHolder {

        TextView txtDuration, txtAverage;
        ExpandableCardView card;

        TrainingViewHolder(View itemView) {
            super(itemView);
            txtDuration = itemView.findViewById(R.id.txtDuration);
            txtAverage = itemView.findViewById(R.id.txtAverage);
            card = itemView.findViewById(R.id.profile);
            card.findViewById(R.id.card).setBackgroundColor(Color.parseColor("#ebeef6"));
        }
    }

    private String trimTrainingDate(Date trainingDate){
        DateFormat dateFormat = new SimpleDateFormat("EEE dd.MM.yyyy HH:mm:ss");
        return dateFormat.format(trainingDate);
    }
}