package com.xu.home.controller.timesheet;

import com.xu.home.param.common.response.Response;
import com.xu.home.utils.common.SessionUtil;
import com.xu.home.param.common.IdPO;
import com.xu.home.domain.timesheet.TimesheetProject;
import com.xu.home.param.timesheet.AddProjectMemberRequest;
import com.xu.home.param.timesheet.CreateProjectRequest;
import com.xu.home.param.timesheet.SyncProjectMembersRequest;
import com.xu.home.service.timesheet.TimesheetProjectService;
import com.xu.home.param.timesheet.vo.ProjectDetailVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目管理接口
 */
@RestController
@RequestMapping("/timesheet/project")
public class TimesheetProjectController {

    private final TimesheetProjectService projectService;

    public TimesheetProjectController(TimesheetProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * 创建工时项目
     */
    @PostMapping("/create")
    public Response<TimesheetProject> createProject(@RequestBody CreateProjectRequest request) {
        return Response.success(projectService.createProject(request, currentAccount(), currentUserName(), SessionUtil.getCurrentPhone()));
    }

    /**
     * 查询当前用户参与的项目
     */
    @GetMapping("/my")
    public Response<List<TimesheetProject>> myProjects() {
        return Response.success(projectService.listMyProjects(currentAccount()));
    }

    /**
     * 查询项目详情和成员列表
     */
    @GetMapping("/detail/{projectId}")
    public Response<ProjectDetailVO> detail(@PathVariable Long projectId) {
        return Response.success(projectService.getDetail(projectId, currentAccount()));
    }

    /**
     * 为项目新增成员
     */
    @PostMapping("/member/add")
    public Response<?> addMember(@RequestBody AddProjectMemberRequest request) {
        return Response.checkResult(projectService.addMember(request, currentAccount()));
    }

    /**
     * 删除未产生记工记录的项目成员
     */
    @PostMapping("/member/remove")
    public Response<?> removeMember(@RequestParam("projectId") Long projectId, @RequestParam("memberId") Long memberId) {
        return Response.checkResult(projectService.removeMember(projectId, memberId, currentAccount()));
    }

    /**
     * 同步当前账号历史项目成员到目标项目，只追加差集成员
     */
    @PostMapping("/member/sync")
    public Response<Integer> syncMembers(@RequestBody SyncProjectMembersRequest request) {
        return Response.success(projectService.syncMembersFromProject(request, currentAccount()));
    }

    /**
     * 删除已结束项目
     */
    @PostMapping("/delete")
    public Response<?> deleteProject(@RequestBody IdPO po) {
        return Response.checkResult(projectService.deleteProject(po.getId(), currentAccount()));
    }

    /**
     * 获取当前登录账号
     */
    private String currentAccount() {
        String account = SessionUtil.getCurrentAccount();
        if (account == null || account.isBlank()) {
            throw new IllegalArgumentException("未登录");
        }
        return account;
    }

    /**
     * 获取当前登录用户名，缺省时回退到账号
     */
    private String currentUserName() {
        String userName = SessionUtil.getCurrentUserName();
        return userName == null || userName.isBlank() ? currentAccount() : userName;
    }
}
