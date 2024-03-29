package com.example.Ships.Game.Controllers;

import com.example.Ships.Game.WinCondition.TTTWinCondition;
import com.example.Ships.Game.Entities.Move;
import com.example.Ships.Game.Entities.TTTGame;
import com.example.Ships.Repo.SimpleRepo2;
import com.example.Ships.Service.UserPrincipal.MyUserPrincipal;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("TTT/{gameID}")
public class TTTBoardController {
    @Autowired
    private SimpleRepo2 repo;
    @GetMapping()
    public String gameBoard(@PathVariable() String gameID, Model model) {//this id is the game id from the database
        if(!isNumeric(gameID) || !hasAccess(gameID)) {//check for validity, check if u have access to this game
            throw new AccessDeniedException("403 returned");
        }
        TTTGame tttGame = repo.findTTTGameByID(Long.parseLong(gameID));
        if(tttGame.getGameStatus().equals("CONCLUDED")) {//game has concluded
            //TODO fix this to see if a player won or the game is a tie
            String results = "Winner is " + repo.getWinnerOfTTTMatch(Long.parseLong(gameID)); //displays the winner of the game
            model.addAttribute("results", results);
        }
        return "TTTGameBoard";
    }
    public boolean isNumeric(String gameID) {//TODO, check for whole number, and large numbers eventually, Add this to static class
        try {
            Integer.parseInt(gameID);
            return true;
        }
        catch(NumberFormatException ex) {
            return false;
        }

    }
    @PostMapping()
    public String rules(@RequestBody(required = true) Map<String, Integer> usermap, @PathVariable String gameID, Model model) {//sent through the body
        //TODO fix model attributes not displaying when calling post
        long gameID2 = Long.parseLong(gameID);//this is from url
        TTTGame tttGame = repo.findTTTGameByID(gameID2);
        int colID = usermap.get("colID");
        int rowID = usermap.get("rowID");
        MyUserPrincipal user = (MyUserPrincipal)(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        if(user.getID() != tttGame.getFirstPlayerID() && user.getID() != tttGame.getSecondPlayerID()) {//prevent access by players not in the game
            throw new AccessDeniedException("403 returned");
        }
        if(tttGame.getGameStatus().equals("CONCLUDED")) {//game has concluded
            String results = "Winner is " + repo.getWinnerOfTTTMatch(gameID2);
            model.addAttribute("results", results);
            return "TTTGameBoard";
        }
        if(tttGame.getGameStatus().equals("HOLD")) {
            String message = "Cannot play game without another Player";
            model.addAttribute("message",message);
            return "TTTGameBoard";
        }
        if(rowID < 1 || rowID > 3 || colID < 1 || colID > 3) {//check for validity of move location
            String message = "Invalid inputs";
            model.addAttribute("message", message);
            return "TTTGameBoard";
        }
        List<Move> moves = repo.getAllMovesByGameID(gameID2);
        if(doesMoveExistInCell(moves, rowID, colID)) { //is cell available
            String message = "Space is taken";
            model.addAttribute("message", message);
            return "TTTGameBoard";
        }
        if(!isPlayersTurn(tttGame, moves)) {//playersturn
            String message = "Not your turn";
            model.addAttribute("message", message);
            return "TTTGameBoard";
        }
        createMove(user, gameID2, colID, rowID, moves, tttGame);//IMPORTANT: create the move based on parameters given
        moves = repo.getAllMovesByGameID(gameID2);//TODO, add later to list instead replacing entire list
        TTTWinCondition tttWinCondition = new TTTWinCondition(moves);
        if(tttWinCondition.isWinner()) {//check win condtion
            repo.conludeTTTGame(gameID2);
            tttWinCondition.getWinner();//this is the character winner
            System.out.println("Winner");
            return "TTTGameBoard";
        }
        if(tttGame.getGameType().equals("BOT")) {//check if this is a bot game if so create bot MOVE
            createBotMove(moves, gameID2);
            moves = repo.getAllMovesByGameID(gameID2);
            tttWinCondition = new TTTWinCondition(moves);
            if(tttWinCondition.isWinner()) {
                repo.conludeTTTGame(gameID2);
                tttWinCondition.getWinner();
                //change the gamestatus to winner;
                //winner, 
                System.out.println(" BOT Winner");
                return "TTTGameBoard";

        }
    }
        if(moves.size() == 9) {//tie
            //TODO add model to say game is concluded
            repo.conludeTTTGame(gameID2);
            return "TTTGameBoard";
        }
        return "TTTGameBoard";
        //Process
        //is cell available?
        //is it players turn
        //create move
        //save move to db
        //check for win conditions
    }
    public void createMove(MyUserPrincipal user, long gameID2, int colID, int rowID, List<Move> moves, TTTGame tttGame) {
        Move newMove = new Move();//create Move
        newMove.setPlayerID(user.getID());
        newMove.setGameID(gameID2);
        newMove.setBoardColumn(colID);
        newMove.setBoardRow(rowID);
        repo.saveMove(newMove);//save move to db

    }
    public void createBotMove(List<Move> moves, long gameID2) {
        while(true) {
            int col = (int)(Math.random()* 3) + 1;
            int row = (int)(Math.random()* 3) + 1;
            if(!doesMoveExistInCell(moves, row, col)) {
                Move botMove = new Move();
                botMove.setBoardColumn(col);
                botMove.setBoardRow(row);
                botMove.setGameID(gameID2);
                repo.saveBOTMove(botMove);
                break;
            }
        }
    }
    public boolean doesMoveExistInCell(List<Move> moves, int rowID, int colID ) {
        for(int x = 0; x < moves.size(); x++) {
            if(moves.get(x).getBoardRow() == rowID && moves.get(x).getBoardColumn() == colID) {//move already exists
                //
                return true;
            }

        }
        return false;

    }
    public boolean isPlayersTurn(TTTGame tttGame, List<Move> moves) {
        char playerPiece = 'a';
        MyUserPrincipal user = (MyUserPrincipal)(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        if(user.getID() == tttGame.getFirstPlayerID()) {
            playerPiece = tttGame.getFirstPlayerPiece();
        }
        else {//are second player
            if(tttGame.getFirstPlayerPiece() == 'O') {
                playerPiece = 'X';

            }
            else {
                playerPiece = 'O';
            }
        }
        int playerTurn = moves.size() % 2;
        if(playerTurn == 0) {//X next
            if(playerPiece == 'X') {
                return true;
            }
            else {
                return false;
            }
        }
        else {//O next
            if(playerPiece == 'X') {
                return false;
            }
            else {
                return true;
            }
        }
    }
    public boolean hasAccess(String gameID) {//check if the player has access to this game
        MyUserPrincipal user = (MyUserPrincipal)(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        Long userID = user.getID();
        TTTGame tttGame = repo.findTTTGameByID(Long.parseLong(gameID));//does the game exist
        if(tttGame == null) {
            return false;
        }
        if(tttGame.getFirstPlayerID() == userID || tttGame.getSecondPlayerID() == userID) {
            return true;
        }
        return false;
    }

}
