package com.future.music.controller;

import com.future.music.entity.dto.GameAnswerDTO;
import com.future.music.entity.dto.GameSessionDTO;
import com.future.music.entity.dto.StartGameDTO;
import com.future.music.entity.message.GameSession;
import com.future.music.entity.vo.GameOneVO;
import com.future.music.service.IGameService;
import entity.pojo.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@Api(tags = "游戏")
@RequestMapping("/game")
public class GameController {


    private final IGameService iGameService;

    public GameController(IGameService iGameService) {
        this.iGameService = iGameService;
    }

    @ApiOperation(value = "开始游戏")
    @PostMapping("/start")
    public Result<GameSession> startGame(@RequestBody @Valid StartGameDTO startGameDTO) {
        return iGameService.startGame(startGameDTO);
    }

    @ApiOperation(value = "获取下一题（单轮答题）")
    @PostMapping("/next")
    public Result<GameOneVO> getNextQuestion(@RequestBody @Valid GameSessionDTO dto) {
        return iGameService.getNextQuestion(dto.getSessionId());
    }

    @ApiOperation(value = "检查答案")
    @PostMapping("/check")
    public Result<Object> checkAnswer(@RequestBody @Valid GameAnswerDTO dto) {
        return iGameService.checkAnswer(dto.getSessionId(), dto.getMusicId(), dto.getAnswer());
    }

    @ApiOperation(value = "结束游戏（清理会话）")
    @PostMapping("/end")
    public Result<Void> endGame(@RequestBody @Valid GameSessionDTO dto) {
        iGameService.endGame(dto.getSessionId());
        return Result.ok();
    }

}
