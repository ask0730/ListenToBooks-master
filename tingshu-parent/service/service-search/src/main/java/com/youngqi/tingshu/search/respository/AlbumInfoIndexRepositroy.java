package com.youngqi.tingshu.search.respository;

import com.youngqi.tingshu.model.search.AlbumInfoIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AlbumInfoIndexRepositroy extends ElasticsearchRepository<AlbumInfoIndex,Long> {
}
