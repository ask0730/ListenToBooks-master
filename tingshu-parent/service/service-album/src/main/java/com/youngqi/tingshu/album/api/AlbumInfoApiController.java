package com.youngqi.tingshu.album.api;

import com.youngqi.tingshu.album.service.AlbumInfoService;
import com.youngqi.tingshu.common.login.YoungQiLogin;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.common.util.AuthContextHolder;
import com.youngqi.tingshu.model.album.AlbumInfo;
import com.youngqi.tingshu.query.album.AlbumInfoQuery;
import com.youngqi.tingshu.vo.album.AlbumInfoVo;
import com.youngqi.tingshu.vo.album.AlbumListVo;
import com.youngqi.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

	@Autowired
	private AlbumInfoService albumInfoService;

	@YoungQiLogin
	@Operation(summary = "新增专辑")
	@PostMapping("/albumInfo/saveAlbumInfo")
	public Result saveAlbumInfo(@RequestBody AlbumInfoVo albumInfoVo) {
		Long userId = AuthContextHolder.getUserId();

		albumInfoService.saveAlbumInfo(userId, albumInfoVo);

		return Result.ok();
	}

	@YoungQiLogin
	@Operation(summary = "查询当前使用用户专辑列表")
	@PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
	public Result findUserAlbumByPage(@PathVariable Integer page, @PathVariable Integer limit, @RequestBody AlbumInfoQuery albumInfoQuery) {
		Long userId = AuthContextHolder.getUserId();
		albumInfoQuery.setUserId(userId);
		Page<AlbumListVo> pageInfo = new Page<>(page, limit);
		pageInfo = albumInfoService.findUserAlbumByPage(pageInfo, albumInfoQuery);
		return Result.ok(pageInfo);
	}

	@YoungQiLogin
	@Operation(summary = "删除专辑")
	@DeleteMapping("/albumInfo/removeAlbumInfo/{id}")
	public Result removeAlbumInfo(@PathVariable Long id) {
		albumInfoService.deletedAlbumInfoById(id);

		return Result.ok();
	}

	@Operation(summary = "用id查专辑（用于回显）")
	@GetMapping("/albumInfo/getAlbumInfo/{id}")
	public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id) {
//		不调单独写缓存的了，调用统一缓存的了
//		AlbumInfo albumInfo = albumInfoService.getAlbumInfo(id);
		AlbumInfo albumInfo = albumInfoService.getAlbumInfoFromDB(id);
		return 	Result.ok(albumInfo);
	}
	@YoungQiLogin
	@Operation(summary = "更新专辑")
	@PutMapping("/albumInfo/updateAlbumInfo/{id}")
	public Result updateAlbumInfo(@RequestBody AlbumInfoVo albumInfoVo,@PathVariable Long id){

		albumInfoService.updateAlbumInfo(albumInfoVo,id);
		return Result.ok();
	}

	@YoungQiLogin
	@Operation(summary = "获取声音页面的所有可添加的专辑")
	@GetMapping("/albumInfo/findUserAllAlbumList")
	public Result<List<AlbumInfo>> findUserAllAlbumList(){
		Long userId = AuthContextHolder.getUserId();
	List<AlbumInfo> list=albumInfoService.findUserAllAlbumList(userId);
		return Result.ok(list);
	}

	/*
	*
	* 根据专辑id查询统计信息
	*
	* */
	@Operation(summary = "根据专辑id查询专辑统计信息")
	@GetMapping("/albumInfo/getAlbumStatVo/{albumId}")
	public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId){
		AlbumStatVo albumStatVo=albumInfoService.getAlbumStatVo(albumId);

		return Result.ok(albumStatVo);
	}

	/**
	 * @param trackId
	 * @return [{name:"本集",price:0.2,"trackCount:1"},{name:"后10集".....},{........}]
	 */
	@YoungQiLogin
	@Operation(summary = "获取用户声音分集购买支付列表")
	@GetMapping("/trackInfo/findUserTrackPaidList/{trackId}")
	public Result<List<Map<String,Object>>>getUserCanPaidPaidList(@PathVariable Long trackId){
		List<Map<String,Object>>userCanPaidPaidList=albumInfoService.getUserCanPaidPaidList(trackId);

		return Result.ok(userCanPaidPaidList);
	}


}

