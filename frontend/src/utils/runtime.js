const runtimeConfig = globalThis.__HELPDESK_RUNTIME_CONFIG__ || {}

function getConfiguredValue(runtimeKey, envKey, fallbackValue) {
  if (Object.prototype.hasOwnProperty.call(runtimeConfig, runtimeKey)) {
    return runtimeConfig[runtimeKey]
  }

  const envValue = import.meta.env[envKey]
  return envValue === undefined ? fallbackValue : envValue
}

export const API_BASE_URL = getConfiguredValue('API_BASE_URL', 'VITE_API_BASE_URL', 'http://localhost:8081/api')
export const FILE_BASE_URL = getConfiguredValue('FILE_BASE_URL', 'VITE_FILE_BASE_URL', 'http://localhost:8081')
