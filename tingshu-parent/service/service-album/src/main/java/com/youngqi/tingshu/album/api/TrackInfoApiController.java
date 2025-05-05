package com.youngqi.tingshu.album.api;

import com.youngqi.tingshu.album.service.TrackInfoService;
import com.youngqi.tingshu.album.service.VodService;
import com.youngqi.tingshu.common.login.YoungQiLogin;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.common.util.AuthContextHolder;
import com.youngqi.tingshu.model.album.TrackInfo;
import com.youngqi.tingshu.query.album.TrackInfoQuery;
import com.youngqi.tingshu.vo.album.AlbumTrackListVo;
import com.youngqi.tingshu.vo.album.TrackInfoVo;
import com.youngqi.tingshu.vo.album.TrackListVo;
import com.youngqi.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

	@Autowired
	private TrackInfoService trackInfoService;
	@Autowired
	private VodService vodService;


	@Operation(summary = "上传声音")
	@PostMapping("/trackInfo/uploadTrack")
	public Result<Map<String,String>> uploadTrack(@RequestBody MultipartFile file){

		Map<String,String> resMap=vodService.uploadTrack(file);

		return Result.ok(resMap);
	}
	@YoungQiLogin
	@Operation(summary = "保存声音")
	@PostMapping("/trackInfo/saveTrackInfo")
	public Result saveTrackInfo(@RequestBody TrackInfoVo trackInfoVo){
		Long userId = AuthContextHolder.getUserId();

		trackInfoService.saveTrackInfo(trackInfoVo,userId);
		return Result.ok();
	}
	@YoungQiLogin
	@Operation(summary = "获取当前用户声音列表（分页）")
	@PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
	public Result<Page<TrackListVo>> findUserTrackPage(@PathVariable Integer page,
													   @PathVariable Integer limit,
													   @RequestBody TrackInfoQuery trackInfoQuery) {
		Long userId = AuthContextHolder.getUserId();
		trackInfoQuery.setUserId(userId);
		Page<TrackListVo> pageInfo = new Page<>(page,limit);
		pageInfo= trackInfoService.findUserTrackPage(pageInfo,trackInfoQuery);
		return Result.ok(pageInfo);
	}

	@Operation(summary = "用声音id拿声音信息（用于回显）")
	@GetMapping("/trackInfo/getTrackInfo/{id}")
	public Result<TrackInfo> getTrackInfo(@PathVariable Long id){

		TrackInfo trackInfo = trackInfoService.getById(id);
			return Result.ok(trackInfo);
	}
	@YoungQiLogin
	@Operation(summary = "修改声音内容")
	@PutMapping("/trackInfo/updateTrackInfo/{id}")
	public Result updateTrackInfo(@RequestBody TrackInfoVo trackInfoVo, @PathVariable Long id){

				trackInfoService.updateTrackInfo(trackInfoVo,id);
		return Result.ok();
	}
	@YoungQiLogin
	@Operation(summary = "删除声音")
	@DeleteMapping("/trackInfo/removeTrackInfo/{id}")
	public Result removeTrackInfo(@PathVariable Long id){
				trackInfoService.deletedTrackInfoById(id);
			return Result.ok();
	}

	@YoungQiLogin(requiredLogin = false)
	@Operation(summary = "根据专辑Id查询专辑声音分页列表,动态根据用户情况展示服务标识")
	@GetMapping("/trackInfo/findAlbumTrackPage/{albumId}/{page}/{limit}")
	public Result<Page<AlbumTrackListVo>>getAlbumTrackPage(@PathVariable Long albumId,@PathVariable int page,@PathVariable int limit){
		//获取当前用户id（或有或无）
		Long userId = AuthContextHolder.getUserId();
		//构建分页对象
		Page<AlbumTrackListVo> pageInfo = new Page<>(page,limit);

		pageInfo=trackInfoService.getAlbumTrackPage(userId,albumId,pageInfo);

		return Result.ok(pageInfo);
	}

	@Operation(summary = "获取声音统计信息")
	@GetMapping("/trackInfo/getTrackStatVo/{trackId}")
	public Result<TrackStatVo> getTrackStatVo(@PathVariable Long trackId){
		TrackStatVo trackStatVo=trackInfoService.getTrackStatVo(trackId);
		return Result.ok(trackStatVo);
	}

	@YoungQiLogin
	@Operation(summary = "查询当前用户待购买声音列表(提供给订单服务渲染购买商品订单列表)")
	@GetMapping("trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
	public Result<List<TrackInfo>> findPaidTrackInfoList(@PathVariable Long trackId,@PathVariable Integer trackCount){
		List<TrackInfo> trackInfoList=trackInfoService.findPaidTrackInfoList(trackId,trackCount);
		return Result.ok(trackInfoList);
	}






}

