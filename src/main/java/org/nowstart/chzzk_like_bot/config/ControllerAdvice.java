package org.nowstart.chzzk_like_bot.config;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@org.springframework.web.bind.annotation.ControllerAdvice
public class ControllerAdvice {
    @ExceptionHandler({IllegalArgumentException.class})
    public ModelAndView handleFavoriteNotFound(IllegalArgumentException e) {
        return new ModelAndView("FavoriteList", "errorMessage", e.getMessage());
    }
}
