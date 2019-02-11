MyCache - интерфейс простого кэша.
MyCacheEhcacheImpl - реализация MyCache на базе решения Ehcache.
MyCacheSimpleImpl - реализация MyCache на базе мап и работы с файлами.
CacheTier - базовый класс "слоя" кэша.
CacheTierMemory - "слой" кэша для работы в ОЗУ на базе мап, используется в MyCacheSimpleImpl.
CacheTierFilesystem - "слой" кэша для работы в файловой системе, используется в MyCacheSimpleImpl.

Настройка MyCacheEhcacheImpl и MyCacheSimpleImpl осуществляется в файле src/main/resources/application.properties