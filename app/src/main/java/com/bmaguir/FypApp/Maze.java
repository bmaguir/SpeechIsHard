package com.bmaguir.FypApp;

import android.text.format.Time;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Brian on 27/02/2015.
 */
public class Maze {

    private MazeCell[][] mMazeCells;
    private int Size;

    public Maze(int size){
        mMazeCells = new MazeCell[size][size];
        Size = size;
        for(int i = 0; i<size; i++){
            for(int j =0; j<size; j++){
                MazeCell north, south, east, west;
                if(j+1<size){
                    north = mMazeCells[i][j+1];}
                else{
                    north = null;}
                if(j-1>0){
                     south = mMazeCells[i][j-1];}
                else{
                     south = null;}
                if(i-1>0){
                     east = mMazeCells[i-1][j];}
                else{
                     east = null;}
                if(i+1<size){
                     west = mMazeCells[i+1][j];}
                else{
                     west = null;}
                mMazeCells[i][j] = new MazeCell(j, i, north, south, east, west);
            }
        }

        fill_maze(0, size/2);
        print_maze();
    }

    private void fill_maze(int y, int x){
        MazeCell currentCell = mMazeCells[y][x];
        ArrayList visitedCells = new ArrayList();
        ArrayList activeCells = new ArrayList();

        while(visitedCells.size() < Size) {

            if(!visitedCells.contains(currentCell)){
                visitedCells.add(currentCell);
                activeCells.add(currentCell);
            }

            MazeCell nextCell = pick_random_direction(currentCell, visitedCells);
            if(nextCell !=  null){
                if(currentCell.x_pos != nextCell.x_pos){  //west-east or north south passage?
                    if(currentCell.x_pos>nextCell.x_pos){   //west or east
                        currentCell.LeftPassage.setPassable(true);
                    }
                    else{
                        nextCell.LeftPassage.setPassable(true);
                    }
                }
                else{
                    if(currentCell.y_pos>nextCell.y_pos){   //north or south
                        currentCell.BottomPassage.setPassable(true);    //north
                    }
                    else{
                        nextCell.BottomPassage.setPassable(true);
                    }

                }
                    currentCell = nextCell;
            }
            else{   //back track!
                    activeCells.remove(activeCells.size()-1);
                    activeCells.get(activeCells.size()-1);
            }
        }

    }

    private MazeCell pick_random_direction(MazeCell currentCell, ArrayList visitedCells){
        Random rand = new Random(Time.SECOND);
        int random = (rand.nextInt()%4);
        MazeCell nextCell = null;
        for(int i =0; i<4; i++) {
            if (currentCell.Neighbours[random] != null && !visitedCells.contains(currentCell.Neighbours[random])) {
                nextCell = currentCell.Neighbours[random];
                break;
            } else {
                nextCell = null;
                random = (random + 1)%4;
            }
        }
        return nextCell;
    }

    private void print_maze(){
        for(int i=0; i< Size; i++){
            for(int j=0; j<Size; j++){
                if(mMazeCells[i][j].BottomPassage.IsPassable)
                    System.out.print(" ");
                else
                    System.out.print("-");
            }
            for(int j=0; j<Size; j++){
                if(mMazeCells[i][j].LeftPassage.IsPassable)
                    System.out.print("  ");
                else
                    System.out.print("! ");
            }
            System.out.print("\n");
        }
    }

    class Passage{
        boolean IsPassable;
        boolean IsNorthSouth;

        public Passage(boolean passable, boolean northsouth){
            IsNorthSouth = northsouth;
            IsPassable = passable;
        }

        public void setPassable(boolean p){
            IsPassable = p;
        }
    }

    class MazeCell{

        public int x_pos;
        public int y_pos;

        private boolean IsExit;

        public Passage LeftPassage;
        public Passage BottomPassage;

        public MazeCell[] Neighbours;

        public MazeCell(int x, int y, MazeCell north, MazeCell south, MazeCell east, MazeCell west){
            x_pos = x;
            y_pos = y;
            IsExit = false;
            LeftPassage = new Passage(false, false);
            BottomPassage = new Passage(false, true);
            Neighbours = new MazeCell[4];

            Neighbours[0] = north;
            Neighbours[1] = south;
            Neighbours[2] = east;
            Neighbours[3] = west;
        }

    }
}

