#!/bin/bash

# SQL Performance Monitoring - Comprehensive Diagnostic Script
# This script checks every component in the monitoring pipeline

echo "=============================================="
echo "SQL Performance Monitoring - Full Diagnostics"
echo "=============================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
    fi
}

# 1. Check Docker Containers
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1. Docker Container Status"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "prometheus|grafana|store"
echo ""

# 2. Check Spring Boot Application
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "2. Spring Boot Application Health"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    print_status 0 "Spring Boot is running on :8080"
    curl -s http://localhost:8080/actuator/health | jq '.'
else
    print_status 1 "Spring Boot is NOT accessible on :8080"
    echo -e "${RED}ACTION REQUIRED: Start your Spring Boot application${NC}"
fi
echo ""

# 3. Check Actuator Prometheus Endpoint
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "3. Spring Boot Actuator Prometheus Endpoint"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if curl -s -f http://localhost:8080/actuator/prometheus > /dev/null 2>&1; then
    print_status 0 "/actuator/prometheus is accessible"
    
    # Count total metrics
    total_metrics=$(curl -s http://localhost:8080/actuator/prometheus | grep -c "^[a-z]")
    echo "  → Total metrics exposed: $total_metrics"
    
    # Check for specific metric families
    echo ""
    echo "  Checking for expected metrics:"
    
    # SQL Performance metrics
    sql_perf_count=$(curl -s http://localhost:8080/actuator/prometheus | grep -c "sql_performance")
    if [ $sql_perf_count -gt 0 ]; then
        print_status 0 "  sql_performance_* metrics found ($sql_perf_count lines)"
    else
        print_status 1 "  sql_performance_* metrics NOT found"
        echo -e "${YELLOW}    → Check if @TrackSqlPerf aspect is working${NC}"
        echo -e "${YELLOW}    → Have you made any API calls to generate metrics?${NC}"
    fi
    
    # HikariCP metrics
    hikari_count=$(curl -s http://localhost:8080/actuator/prometheus | grep -c "hikaricp")
    if [ $hikari_count -gt 0 ]; then
        print_status 0 "  hikaricp_* metrics found ($hikari_count lines)"
    else
        print_status 1 "  hikaricp_* metrics NOT found"
    fi
    
    # JVM metrics
    jvm_count=$(curl -s http://localhost:8080/actuator/prometheus | grep -c "jvm_")
    if [ $jvm_count -gt 0 ]; then
        print_status 0 "  jvm_* metrics found ($jvm_count lines)"
    else
        print_status 1 "  jvm_* metrics NOT found"
    fi
    
else
    print_status 1 "/actuator/prometheus is NOT accessible"
    echo -e "${RED}ACTION REQUIRED: Enable actuator prometheus endpoint${NC}"
    echo -e "${YELLOW}Add to application.yml:${NC}"
    echo "  management:"
    echo "    endpoints:"
    echo "      web:"
    echo "        exposure:"
    echo "          include: prometheus,health,metrics"
fi
echo ""

# 4. Check Prometheus Targets
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "4. Prometheus Scraping Status"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if curl -s -f http://localhost:9090/api/v1/targets > /dev/null 2>&1; then
    print_status 0 "Prometheus is accessible on :9090"
    
    echo ""
    echo "  Target Status:"
    curl -s http://localhost:9090/api/v1/targets | jq -r '.data.activeTargets[] | "  → \(.job): \(.health) | \(.scrapeUrl) | Last: \(.lastScrape)"'
    
    echo ""
    echo "  Checking for DOWN targets:"
    down_targets=$(curl -s http://localhost:9090/api/v1/targets | jq -r '.data.activeTargets[] | select(.health != "up") | .job')
    if [ -z "$down_targets" ]; then
        print_status 0 "  All targets are UP"
    else
        print_status 1 "  Some targets are DOWN:"
        echo "$down_targets" | while read job; do
            echo "    - $job"
            curl -s http://localhost:9090/api/v1/targets | jq -r ".data.activeTargets[] | select(.job == \"$job\") | \"      Error: \(.lastError)\""
        done
        echo -e "${YELLOW}ACTION REQUIRED: Fix DOWN targets${NC}"
    fi
else
    print_status 1 "Prometheus is NOT accessible on :9090"
    echo -e "${RED}ACTION REQUIRED: Check if Prometheus container is running${NC}"
fi
echo ""

# 5. Check if Prometheus has metrics
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "5. Prometheus Metrics Storage"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if curl -s -f http://localhost:9090/api/v1/label/__name__/values > /dev/null 2>&1; then
    print_status 0 "Prometheus is storing metrics"
    
    total_metrics=$(curl -s http://localhost:9090/api/v1/label/__name__/values | jq '.data | length')
    echo "  → Total unique metrics in Prometheus: $total_metrics"
    
    echo ""
    echo "  Checking for expected metric names in Prometheus:"
    
    # Check for sql_performance metrics
    sql_perf_prometheus=$(curl -s 'http://localhost:9090/api/v1/query?query=sql_performance_duration_seconds_count' | jq -r '.data.result | length')
    if [ "$sql_perf_prometheus" != "0" ]; then
        print_status 0 "  sql_performance_duration_seconds_count found in Prometheus"
    else
        print_status 1 "  sql_performance_duration_seconds_count NOT in Prometheus"
        echo -e "${YELLOW}    → Metric not generated by Spring Boot OR not scraped yet${NC}"
    fi
    
    # Check for hikaricp metrics
    hikari_prometheus=$(curl -s 'http://localhost:9090/api/v1/query?query=hikaricp_connections_active' | jq -r '.data.result | length')
    if [ "$hikari_prometheus" != "0" ]; then
        print_status 0 "  hikaricp_connections_active found in Prometheus"
    else
        print_status 1 "  hikaricp_connections_active NOT in Prometheus"
    fi
    
    # Check for jvm metrics
    jvm_prometheus=$(curl -s 'http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes' | jq -r '.data.result | length')
    if [ "$jvm_prometheus" != "0" ]; then
        print_status 0 "  jvm_memory_used_bytes found in Prometheus"
    else
        print_status 1 "  jvm_memory_used_bytes NOT in Prometheus"
    fi
    
    echo ""
    echo "  Sample metrics in Prometheus (first 10):"
    curl -s http://localhost:9090/api/v1/label/__name__/values | jq -r '.data[:10][]' | sed 's/^/    - /'
    
else
    print_status 1 "Cannot query Prometheus metrics"
fi
echo ""

# 6. Check Grafana
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "6. Grafana Configuration"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if curl -s -f -u admin:admin http://localhost:3000/api/health > /dev/null 2>&1; then
    print_status 0 "Grafana is accessible on :3000"
    
    # Check datasources
    echo ""
    echo "  Configured Datasources:"
    datasources=$(curl -s -u admin:admin http://localhost:3000/api/datasources)
    echo "$datasources" | jq -r '.[] | "  → \(.name) (\(.type)): \(.url) | Default: \(.isDefault)"'
    
    # Test datasource
    echo ""
    echo "  Testing Prometheus Datasource:"
    datasource_test=$(curl -s -u admin:admin http://localhost:3000/api/datasources/name/Prometheus | jq -r '.id')
    if [ "$datasource_test" != "null" ] && [ -n "$datasource_test" ]; then
        print_status 0 "  Prometheus datasource exists (ID: $datasource_test)"
        
        # Try to query through Grafana
        query_result=$(curl -s -X POST \
            -u admin:admin \
            -H "Content-Type: application/json" \
            http://localhost:3000/api/datasources/proxy/$datasource_test/api/v1/query \
            -d '{"query":"up"}' | jq -r '.status')
        
        if [ "$query_result" = "success" ]; then
            print_status 0 "  Grafana can query Prometheus successfully"
        else
            print_status 1 "  Grafana CANNOT query Prometheus"
            echo -e "${YELLOW}    → Network issue between Grafana and Prometheus${NC}"
        fi
    else
        print_status 1 "  Prometheus datasource NOT configured"
        echo -e "${RED}ACTION REQUIRED: Check datasource provisioning${NC}"
    fi
    
    # Check dashboards
    echo ""
    echo "  Imported Dashboards:"
    curl -s -u admin:admin http://localhost:3000/api/search?type=dash-db | jq -r '.[] | "  → \(.title) (UID: \(.uid))"'
    
else
    print_status 1 "Grafana is NOT accessible on :3000"
    echo -e "${RED}ACTION REQUIRED: Check if Grafana container is running${NC}"
fi
echo ""

# 7. Check Grafana Provisioning Files
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "7. Grafana Provisioning Files (in container)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if docker ps | grep -q grafana; then
    echo "  Datasource provisioning files:"
    docker exec grafana ls -la /etc/grafana/provisioning/datasources/ 2>/dev/null || echo "    Directory not found or empty"
    
    echo ""
    echo "  Dashboard provisioning files:"
    docker exec grafana ls -la /etc/grafana/provisioning/dashboards/ 2>/dev/null || echo "    Directory not found or empty"
    
    echo ""
    echo "  Checking datasource file content:"
    if docker exec grafana test -f /etc/grafana/provisioning/datasources/grafana-datasource.yml 2>/dev/null; then
        print_status 0 "  grafana-datasource.yml exists in container"
    else
        print_status 1 "  grafana-datasource.yml NOT found in container"
        echo -e "${RED}ACTION REQUIRED: Check docker-compose volume mounts${NC}"
    fi
else
    print_status 1 "Grafana container not running"
fi
echo ""

# 8. Network Connectivity Test
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "8. Network Connectivity Tests"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if docker ps | grep -q prometheus; then
    echo "  Testing Prometheus → Spring Boot:"
    if docker exec prometheus wget -q -O- http://host.docker.internal:8080/actuator/health 2>/dev/null | grep -q "UP"; then
        print_status 0 "  Prometheus can reach Spring Boot"
    else
        print_status 1 "  Prometheus CANNOT reach Spring Boot"
        echo -e "${YELLOW}    → Check if Spring Boot is running${NC}"
        echo -e "${YELLOW}    → Try using 'localhost:8080' in prometheus.yml if running without Docker${NC}"
    fi
fi

if docker ps | grep -q grafana; then
    echo ""
    echo "  Testing Grafana → Prometheus:"
    if docker exec grafana wget -q -O- http://prometheus:9090/api/v1/query?query=up 2>/dev/null | grep -q "success"; then
        print_status 0 "  Grafana can reach Prometheus"
    else
        print_status 1 "  Grafana CANNOT reach Prometheus"
        echo -e "${RED}ACTION REQUIRED: Check docker network configuration${NC}"
    fi
fi
echo ""

# 9. Summary and Recommendations
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "9. Summary & Recommendations"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo ""
echo "Quick Action Commands:"
echo ""
echo "  # View Prometheus targets status"
echo "  curl http://localhost:9090/targets"
echo ""
echo "  # Check metrics in actuator"
echo "  curl http://localhost:8080/actuator/prometheus | grep sql_performance | head -10"
echo ""
echo "  # Query Prometheus directly"
echo "  curl 'http://localhost:9090/api/v1/query?query=sql_performance_duration_seconds_count'"
echo ""
echo "  # Check Grafana datasources"
echo "  curl -u admin:admin http://localhost:3000/api/datasources | jq"
echo ""
echo "  # View container logs"
echo "  docker logs grafana --tail 50"
echo "  docker logs prometheus --tail 50"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Diagnostic Complete!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
