import { render } from "./view";
import type { Props } from "./types";
import * as fs from "fs";
import "./polyfill";
import Default, { helper as h } from "./lib";

const p: Props = { count: h() };
render(p);
