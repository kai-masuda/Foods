package com.example.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.entity.Unit;
import com.example.repository.UnitRepository;

@Service
public class UnitService {
    
    @Autowired
    private UnitRepository unitRepository;

    //単位一覧取得
    public List<Unit> getAllUnits() {
        return unitRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }
    
    //単位の保存
    public Unit saveCategory(Unit unit) {
        return unitRepository.save(unit);
    }
    
    //単位検索or（なければ）自動生成
    public Unit getOrCreateUnit(String unitName) {
        //単位が空なら個
        if (unitName == null || unitName.trim().isEmpty()) {
            unitName = "個";
        }
        
        //単位の前後の余計な空白を削る
        String trimmedName = unitName.trim();
        
        //単位が既存か探す
        List<Unit> allUnits = unitRepository.findAll();
        for(Unit kizonUnit: allUnits) {
            if(trimmedName.equals(kizonUnit.getUnitName())) {
                return kizonUnit;
                
            }
        }
        Unit newUnit = new Unit();
        newUnit.setUnitName(trimmedName);
        
        return unitRepository.save(newUnit);
    }
    

}
