/*
 * Copyright 2017, OpenSkywalking Organization All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project repository: https://github.com/OpenSkywalking/skywalking
 */

package org.skywalking.apm.agent.core.jvm.memorypool;

import org.skywalking.apm.network.proto.MemoryPool;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;

/**
 * @author wusheng
 */
public enum MemoryPoolProvider {
    INSTANCE;

    private MemoryPoolMetricAccessor metricAccessor;
    private List<MemoryPoolMXBean> beans;

    MemoryPoolProvider() {
        // 创建 JVM GC 方式对应的 MemoryPoolMetricAccessor 对象
        beans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean bean : beans) {
            String name = bean.getName();
            MemoryPoolMetricAccessor accessor = findByBeanName(name);
            if (accessor != null) {
                metricAccessor = accessor;
                break;
            }
        }
        if (metricAccessor == null) {
            metricAccessor = new UnknownMemoryPool();
        }
    }

    public List<MemoryPool> getMemoryPoolMetricList() {
        return metricAccessor.getMemoryPoolMetricList();
    }

    private MemoryPoolMetricAccessor findByBeanName(String name) {
        if (name.indexOf("PS") > -1) { // ParallelCollector
            //Parallel (Old) collector ( -XX:+UseParallelOldGC )
            return new ParallelCollectorModule(beans);
        } else if (name.indexOf("CMS") > -1) { // CMSCollector
            // CMS collector ( -XX:+UseConcMarkSweepGC )
            return new CMSCollectorModule(beans);
        } else if (name.indexOf("G1") > -1) { // G1Collector
            // G1 collector ( -XX:+UseG1GC )
            return new G1CollectorModule(beans);
        } else if (name.equals("Survivor Space")) { // SerialCollector
            // Serial collector ( -XX:+UseSerialGC )
            return new SerialCollectorModule(beans);
        } else {
            // Unknown
            return null;
        }
    }
}
