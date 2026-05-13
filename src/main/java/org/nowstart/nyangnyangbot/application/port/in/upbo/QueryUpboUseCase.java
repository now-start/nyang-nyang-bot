package org.nowstart.nyangnyangbot.application.port.in.upbo;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UserUpboResult;

public interface QueryUpboUseCase {

    List<UserUpboResult> getUserUpbos(String userId, String status);
}
