package com.resky.yuaicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.resky.yuaicodemother.mapper.AiModelUsageMapper;
import com.resky.yuaicodemother.model.entity.AiModelUsage;
import com.resky.yuaicodemother.service.AiModelUsageService;
import org.springframework.stereotype.Service;

@Service
public class AiModelUsageServiceImpl extends ServiceImpl<AiModelUsageMapper, AiModelUsage> implements AiModelUsageService {}
