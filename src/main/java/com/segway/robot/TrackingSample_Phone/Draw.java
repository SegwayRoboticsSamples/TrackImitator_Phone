package com.segway.robot.TrackingSample_Phone;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;

public class Draw extends View {

    private int mapWidth;
    private int mapHeight;
    private int gridWidthInPixel;
    private static final int GRID_COL = 5;
    private static final int GRID_ROW = 3;
    private float mStartX;
    private float mStartY;
    private float mEndX;
    private float mEndY;
    private Paint mPaintTrack;
    private Paint mPaintJoint;
    private Paint mPaintGrid;
    private Paint mPaintText;
    private Bitmap mBitmap;
    private Canvas mCanvas;

    private LinkedList<PointF> mLinkedList;

    public Draw(Context context, int width, int height, float density, int gridWidthInPixel) {
        super(context);
        this.mapWidth = width - (int)(100*density);
        this.mapHeight = height - (int)(60*density);
        this.gridWidthInPixel = gridWidthInPixel;

        mLinkedList = new LinkedList<>();

        // init canvas
        mBitmap = Bitmap.createBitmap(mapWidth, mapHeight, Bitmap.Config.RGB_565);
        mCanvas = new Canvas(mBitmap);

        // trajectory draw pen
        mPaintTrack = new Paint(Paint.DITHER_FLAG);
        mPaintTrack.setStyle(Paint.Style.STROKE);
        mPaintTrack.setStrokeWidth(3);
        mPaintTrack.setColor(Color.RED);
        mPaintTrack.setAntiAlias(true);
        mPaintTrack.setDither(true);
        mPaintTrack.setStrokeJoin(Paint.Join.ROUND);
        mPaintTrack.setStrokeCap(Paint.Cap.ROUND);

        // joint draw pen
        mPaintJoint = new Paint(Paint.DITHER_FLAG);
        mPaintJoint.setStyle(Paint.Style.STROKE);
        mPaintJoint.setStrokeWidth(5);
        mPaintJoint.setColor(Color.GREEN);
        mPaintJoint.setAntiAlias(true);
        mPaintJoint.setDither(true);
        mPaintJoint.setStrokeJoin(Paint.Join.ROUND);
        mPaintJoint.setStrokeCap(Paint.Cap.ROUND);

        // grid draw pen
        mPaintGrid = new Paint(Paint.DITHER_FLAG);
        mPaintGrid.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaintGrid.setStrokeWidth(1);
        mPaintGrid.setColor(Color.GRAY);
        mPaintGrid.setAntiAlias(true);
        mPaintGrid.setDither(true);
        mPaintGrid.setStrokeJoin(Paint.Join.ROUND);
        mPaintGrid.setStrokeCap(Paint.Cap.ROUND);
        mPaintGrid.setTextSize(mapHeight/25);

        // text draw pen
        mPaintText = new Paint(Paint.LINEAR_TEXT_FLAG);
        mPaintText.setStyle(Paint.Style.FILL);
        mPaintText.setColor(Color.WHITE);
        mPaintText.setAntiAlias(true);
        mPaintText.setDither(true);
        mPaintText.setTextSize(mapHeight/25);

        // draw grid
        drawGrid();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, mPaintTrack);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            resetPaint();
            mLinkedList.clear();
            mStartX = event.getX();
            mStartY = event.getY();
            mLinkedList.add(new PointF(mStartX, mStartY));
            mCanvas.drawPoint(mStartX, mStartY, mPaintTrack);
            mCanvas.drawText("start", mStartX, mStartY, mPaintText);
        }

        if(event.getAction() == MotionEvent.ACTION_MOVE) {
            mEndX = event.getX();
            mEndY = event.getY();
            mCanvas.drawPoint(mEndX, mEndY, mPaintJoint);
            mLinkedList.add(new PointF(mEndX, mEndY));
            mCanvas.drawLine(mStartX, mStartY, mEndX, mEndY, mPaintTrack);
            mStartX = mEndX;
            mStartY = mEndY;
        }

        if(event.getAction() == MotionEvent.ACTION_UP) {
            mCanvas.drawText("end", mStartX, mStartY, mPaintText);
        }

        invalidate();

        return true;
    }

    // reset paint to empty
    public void resetPaint() {
        mBitmap.recycle();
        mBitmap = Bitmap.createBitmap(mapWidth, mapHeight, Bitmap.Config.RGB_565);
        mCanvas = new Canvas(mBitmap);
        drawGrid();

        this.invalidate();
    }

    // get point list in time order
    public LinkedList<PointF> getPointList() {
        return mLinkedList;
    }

    // get canvas width
    public int getCanvasWidth() {
        return mapWidth;
    }

    // get canvas height
    public int getCanvasHeight() {
        return mapHeight;
    }

    // draw grid line
    private void drawGrid() {
        // draw grid
        for(int i = 1; i*gridWidthInPixel < mapWidth; i++) {
            mCanvas.drawLine(gridWidthInPixel*i, 0, gridWidthInPixel*i, mapHeight, mPaintGrid);
        }
        for(int j = 1; j*gridWidthInPixel < mapHeight; j++) {
            mCanvas.drawLine(0, gridWidthInPixel*j, mapWidth, gridWidthInPixel*j, mPaintGrid);
        }

        // draw direction hint
        mCanvas.drawText("front", mapWidth/2, mPaintText.getTextSize(), mPaintGrid);
        mCanvas.drawText("rear", mapWidth/2, mapHeight - mPaintText.getTextSize(), mPaintGrid);
        mCanvas.drawText("left", 0, mapHeight/2, mPaintGrid);
        mCanvas.drawText("right", mapWidth - mPaintText.getTextSize()*3, mapHeight/2, mPaintGrid);
    }
}
