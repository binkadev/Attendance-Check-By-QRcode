import { classApi } from './src/api/classApi.js';
classApi.getTeachingClasses().then(res => console.log(JSON.stringify(res, null, 2))).catch(e => console.error(e));
