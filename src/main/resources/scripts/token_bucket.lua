-- token_bucket.lua
--
-- WHY THIS FILE EXISTS:
-- A naive Java implementation does 3 separate steps: READ tokens, CHECK if
-- enough, WRITE the new value. Two requests can both READ the same value
-- before either WRITEs, so both pass the CHECK -- letting through more
-- requests than the limit allows. That's the race condition.
--
-- THE FIX:
-- Redis runs each Lua script as ONE atomic operation -- no other command,
-- from any client, can run in the middle of this script. So the
-- read-check-write sequence becomes one indivisible step, with no gap left
-- for two requests to interleave.
--
-- KEYS[1] = the Redis key for this client/route's bucket
-- ARGV[1] = capacity            (max tokens the bucket can hold)
-- ARGV[2] = refillTokensPerSec  (tokens added per second, can be fractional)
-- ARGV[3] = nowMillis           (current time, passed in from Java)
-- ARGV[4] = requestedTokens     (how many tokens this request needs, usually 1)
--
-- Returns: { tokensRemainingAfterThisCall, allowed (1 or 0) }

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillPerSec = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local bucket = redis.call("HMGET", key, "tokens", "lastRefillMs")

local tokens
local lastRefillMs

if bucket[1] == false then
    tokens = capacity
    lastRefillMs = now
else
    tokens = tonumber(bucket[1])
    lastRefillMs = tonumber(bucket[2])
end

local elapsedSeconds = math.max(0, (now - lastRefillMs) / 1000.0)
local refillAmount = elapsedSeconds * refillPerSec
tokens = math.min(capacity, tokens + refillAmount)

local allowed = 0
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end

redis.call("HMSET", key, "tokens", tostring(tokens), "lastRefillMs", tostring(now))
redis.call("EXPIRE", key, 3600)

return { tostring(tokens), allowed }