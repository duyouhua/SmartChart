package com.daivd.chart.provider;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import com.daivd.chart.axis.AxisDirection;
import com.daivd.chart.data.LineData;
import com.daivd.chart.data.ScaleData;
import com.daivd.chart.data.style.LineStyle;
import com.daivd.chart.data.style.PointStyle;
import com.daivd.chart.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by huang on 2017/9/26.
 */

public abstract  class BaseLineProvider extends BarLineProvider {

    private LineStyle lineStyle = new LineStyle();

    private PointStyle pointStyle = new PointStyle();

    private boolean isShowPoint;
    private  boolean isArea; //是否打开面积图
    private boolean isDrawLine = true;

    @Override
    public void drawProvider(Canvas canvas, Rect zoomRect, Rect rect,Paint paint) {

        ScaleData scaleData= chartData.getScaleData();
        List<LineData> columnDatas = chartData.getColumnDataList();
        int columnSize = columnDatas.size();
        int rowSize = chartData.getCharXDataList().size();
        double width = (zoomRect.right - zoomRect.left)/(rowSize-1);
        float height =(zoomRect.bottom - zoomRect.top);

        for(int i = 0;i <columnSize;i++){
            LineData columnData = columnDatas.get(i);
            paint.setColor(columnData.getColor());
            if(!columnData.isDraw()){
                continue;
            }
            List<Float> pointX = new ArrayList<>();
            List<Float> pointY = new ArrayList<>();
            List<Double> datas = columnData.getChartYDataList();
            for(int j = 0;j <rowSize;j++){
                double value = datas.get(j);
                float x = (float) (j*width + zoomRect.left);
                float y = getAnimValue(getStartY(zoomRect, scaleData, height, value,columnData.getDirection()));
                pointX.add(x);
                pointY.add(y);
            }

            lineStyle.fillPaint(paint);
            paint.setColor(columnData.getColor());
            drawLine(canvas, rect, pointX, pointY,paint);

        }
        if(levelLine != null && levelLine.isDraw()) {
            float startY = getStartY(zoomRect,scaleData,height,levelLine.getValue(),levelLine.getDirection());
            drawLevelLine(canvas, zoomRect,startY,paint);
        }
    }

    private void drawLine(Canvas canvas,Rect rect, List<Float> pointX, List<Float> pointY,Paint paint) {
       if(isDrawLine) {
           Path path = getLinePath(pointX, pointY);
           if (isArea) {
               paint.setStyle(Paint.Style.FILL);
               path.lineTo(rect.right, rect.bottom);
               path.lineTo(rect.left, rect.bottom);
               path.close();
               int alphaColor =  ColorUtils.changeAlpha(paint.getColor(),125);
               paint.setColor(alphaColor);
           }
           canvas.drawPath(path, paint);
       }
    }

    @Override
    protected void drawPeripheral(Canvas canvas, Rect zoomRect, Rect rect, Paint paint) {
        ScaleData scaleData= chartData.getScaleData();
        List<LineData> columnDatas = chartData.getColumnDataList();
        int columnSize = columnDatas.size();
        int rowSize = chartData.getCharXDataList().size();
        double width = (zoomRect.right - zoomRect.left)/(rowSize-1);
        float height =(zoomRect.bottom - zoomRect.top);
        List<Float> pointXList = new ArrayList<>();
        List<Float> pointYList = new ArrayList<>();

        for(int i = 0;i <columnSize;i++){
            LineData columnData = columnDatas.get(i);
            paint.setColor(columnData.getColor());
            if(!columnData.isDraw()){
                continue;
            }
            List<Float> pointX = new ArrayList<>();
            List<Float> pointY = new ArrayList<>();
            List<Double> datas = columnData.getChartYDataList();
            for(int j = 0;j <rowSize;j++){
                double value = datas.get(j);
                float x = (float) (j*width + zoomRect.left);
                float y = getAnimValue(getStartY(zoomRect, scaleData, height, value,columnData.getDirection()));
                if(rect.contains((int)x,(int)y)) {
                    pointX.add(x);
                    pointY.add(y);
                    drawPointText(x, y, canvas, paint, value);
                }
            }
            lineStyle.fillPaint(paint);
            paint.setColor(columnData.getColor());
            drawPoint(canvas,pointX,pointY,paint);
            pointXList.addAll(pointX);
            pointYList.addAll(pointY);
        }
        drawClickCross(canvas,rect,pointXList,pointYList,paint);
    }

    private float getStartY(Rect zoomRect, ScaleData scaleData, float height, double value, AxisDirection direction) {
        return (float) (zoomRect.bottom - (value - scaleData.getMinScaleValue(direction))*height/scaleData.getTotalScaleLength(direction));
    }


    private void drawClickCross(Canvas canvas, Rect rect, List<Float> pointX, List<Float> pointY, Paint paint){
        if(pointX.size() ==0){
            return;
        }
       if(pointF != null  && rect.contains((int)pointF.x,(int)pointF.y)&& (isShowPoint() || isOpenMark())) {
           int minPosition = 0;
           float minValue = -1;
           for (int i = 0; i < pointX.size(); i++) {
               float x = pointX.get(i);
               float y= pointY.get(i);
               float disX = Math.abs(pointF.x - x);
               float disY = Math.abs(pointF.y - y);
                float dis = disX + disY;
               if(minValue == -1 || dis < minValue){
                   minValue = dis;
                   minPosition = i;
               }
           }
           float centerX = pointX.get(minPosition);
           float centerY = pointY.get(minPosition);
           if(isOpenCross()) {
               crossStyle.fillPaint(paint);
               canvas.drawLine(centerX, rect.top, centerX, rect.bottom, paint);
               canvas.drawLine(rect.left, centerY, rect.right, centerY, paint);
           }
           if(markView != null && isOpenMark()){
               List<String> groupList = chartData.getCharXDataList();
               int columnPos = minPosition/groupList.size();
               int rowPos = minPosition % groupList.size();
               LineData columnData = chartData.getColumnDataList().get(columnPos);
               markView.drawMark(centerX,centerY,groupList.get(rowPos),
                       columnData,rowPos);
           }
       }
    }

    protected void drawPoint(Canvas canvas,List<Float> pointX, List<Float> pointY,Paint paint){

        if(isShowPoint && pointStyle != null && pointStyle.isDraw()) {
            float w = pointStyle.getWidth();
            for(int i = 0; i < pointY.size();i++) {
                float x = pointX.get(i);
                float y = pointY.get(i);
                int oldColor = paint.getColor();
                pointStyle.fillPaint(paint);
                paint.setColor(oldColor);
                if(pointStyle.getShape() == PointStyle.CIRCLE) {
                    canvas.drawCircle(x, y, w, paint);
                }else if(pointStyle.getShape() == PointStyle.SQUARE){
                    canvas.drawRect(x-w/2,y-w/2,x+w/2,y+w/2,paint);
                }else if(pointStyle.getShape() == PointStyle.RECT){
                    canvas.drawRect(x-w*2/3,y-w/2,w*2/3,y+w/2,paint);
                }
            }
        }
    }

   protected  abstract Path getLinePath(List<Float> pointX, List<Float> pointY);

    public LineStyle getLineStyle() {
        return lineStyle;
    }

    public PointStyle getPointStyle() {
        return pointStyle;
    }



    @Override
    public double[] setMaxMinValue(double maxValue, double minValue) {
        double dis = Math.abs(maxValue -minValue);
         maxValue = maxValue + dis*0.3;
            if(minValue >0){
                minValue = 0;
            }else{
                minValue = minValue - dis*0.3;
            }
            return new double[]{maxValue,minValue};
        }

    public boolean isShowPoint() {
        return isShowPoint;
    }

    public void setShowPoint(boolean showPoint) {
        isShowPoint = showPoint;
    }



    public void setArea(boolean isArea) {
        this.isArea = isArea;
    }



    public void setDrawLine(boolean drawLine) {
        isDrawLine = drawLine;
    }
}
