#!/bin/bash

# Start SQL Server in the background
/opt/mssql/bin/sqlservr &

# Wait for SQL Server to start (increased from 30s to 60s)
echo "Waiting for SQL Server to start..."
echo "This may take up to 60 seconds on first startup..."
sleep 60s

# Test connection before running scripts
MAX_RETRIES=5
RETRY_COUNT=0
echo "Testing SQL Server connection..."

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -Q "SELECT 1" &> /dev/null; then
        echo "SQL Server is ready!"
        break
    else
        RETRY_COUNT=$((RETRY_COUNT + 1))
        echo "Connection attempt $RETRY_COUNT/$MAX_RETRIES failed. Waiting 10 seconds..."
        sleep 10s
    fi
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "ERROR: Failed to connect to SQL Server after $MAX_RETRIES attempts!"
    echo "To restart the container, run:"
    echo "  docker-compose restart database"
    echo "Or recreate it with:"
    echo "  docker-compose down database"
    echo "  docker volume rm backend_sqlserver-data"
    echo "  docker-compose up -d database"
    exit 1
fi

# Run initialization scripts
echo "Running initialization scripts..."
SCRIPT_ERROR=0

for script in /docker-entrypoint-initdb.d/*.sql; do
    if [ -f "$script" ]; then
        echo "Executing $script..."
        if /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -i "$script"; then
            echo "✓ Successfully executed $script"
        else
            echo "✗ ERROR: Failed to execute $script"
            SCRIPT_ERROR=1
        fi
    fi
done

if [ $SCRIPT_ERROR -eq 1 ]; then
    echo ""
    echo "ERROR: One or more initialization scripts failed!"
    echo "Check the logs above for details."
    echo ""
    echo "To fix and restart:"
    echo "  1. Fix the SQL script errors"
    echo "  2. docker-compose down database"
    echo "  3. docker volume rm backend_sqlserver-data"
    echo "  4. docker-compose up -d database"
    exit 1
fi

echo "✓ Initialization complete!"
echo "Database is ready for connections on port 1433"

# Keep SQL Server running in the foreground
wait
