package be.uantwerpen.sc.repositories.newMap;

import be.uantwerpen.sc.models.map.newMap.Point;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointRepository extends CrudRepository<Point, Long> {


    List<Point> findAll();
}