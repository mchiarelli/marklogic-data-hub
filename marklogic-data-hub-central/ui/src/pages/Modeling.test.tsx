import React from "react";
import {render, wait, screen, fireEvent} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {BrowserRouter as Router} from "react-router-dom";

import Modeling from "./Modeling";
import {AuthoritiesContext} from "../util/authorities";
import authorities from "../assets/mock-data/authorities.testutils";
import {ModelingContext} from "../util/modeling-context";
import {ModelingTooltips} from "../config/tooltips.config";
import {getEntityTypes} from "../assets/mock-data/modeling/modeling";
import {isModified, notModified} from "../assets/mock-data/modeling/modeling-context-mock";
import {primaryEntityTypes, updateEntityModels} from "../api/modeling";
import {ConfirmationType} from "../types/common-types";
import tiles from "../config/tiles.config";
import {getViewSettings} from "../util/user-context";

jest.mock("../api/modeling");

const mockPrimaryEntityType = primaryEntityTypes as jest.Mock;
const mockUpdateEntityModels = updateEntityModels as jest.Mock;

const mockDevRolesService = authorities.DeveloperRolesService;
const mockOpRolesService = authorities.OperatorRolesService;
const mockHCUserRolesService = authorities.HCUserRolesService;

describe("Modeling Page", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  test("Modeling: with mock data, renders modified Alert component and Dev role can click add, edit, and save all", async () => {
    mockPrimaryEntityType.mockResolvedValueOnce({status: 200, data: getEntityTypes});
    mockUpdateEntityModels.mockResolvedValueOnce({status: 200});

    const {getByText, getByLabelText} = render(
      <AuthoritiesContext.Provider value={mockDevRolesService}>
        <ModelingContext.Provider value={isModified}>
          <Router>
            <Modeling/>
          </Router>
        </ModelingContext.Provider>
      </AuthoritiesContext.Provider>
    );

    await wait(() => expect(mockPrimaryEntityType).toHaveBeenCalledTimes(1));

    expect(getByText(tiles.model.intro)).toBeInTheDocument(); // tile intro text

    expect(getByText("Entity Types")).toBeInTheDocument();
    expect(getByLabelText("add-entity")).toBeInTheDocument();
    expect(getByText("Instances")).toBeInTheDocument();
    expect(getByText("Last Processed")).toBeInTheDocument();
    expect(getByText(ModelingTooltips.entityEditedAlert)).toBeInTheDocument();

    // test add, save, revert icons display correct tooltip when enabled
    fireEvent.mouseOver(getByText("Add"));
    await wait(() => expect(getByText(ModelingTooltips.addNewEntity)).toBeInTheDocument());
    fireEvent.mouseOver(getByText("Save All"));
    await wait(() => expect(getByText(ModelingTooltips.saveAll)).toBeInTheDocument());
    fireEvent.mouseOver(getByText("Revert All"));
    await wait(() => expect(getByText(ModelingTooltips.revertAll)).toBeInTheDocument());

    userEvent.click(screen.getByTestId("AnotherModel-span"));
    expect(screen.getByText("Edit Entity Type")).toBeInTheDocument();

    userEvent.click(getByText("Add"));
    expect(getByText(/Add Entity Type/i)).toBeInTheDocument();

    userEvent.click(getByText("Save All"));
    userEvent.click(screen.getByLabelText(`confirm-${ConfirmationType.SaveAll}-yes`));
    expect(mockUpdateEntityModels).toHaveBeenCalledTimes(1);

    userEvent.click(getByText("Revert All"));
    userEvent.click(screen.getByLabelText(`confirm-${ConfirmationType.RevertAll}-yes`));
    expect(mockPrimaryEntityType).toHaveBeenCalledTimes(1);
  });

  test("Modeling: with mock data, no Alert component renders and operator role can not click add", async () => {
    mockPrimaryEntityType.mockResolvedValueOnce({status: 200, data: getEntityTypes});

    const {getByText, getByLabelText, queryByLabelText} = render(
      <AuthoritiesContext.Provider value={mockOpRolesService}>
        <ModelingContext.Provider value={notModified}>
          <Router>
            <Modeling/>
          </Router>
        </ModelingContext.Provider>
      </AuthoritiesContext.Provider>
    );

    await wait(() => expect(mockPrimaryEntityType).toHaveBeenCalledTimes(1));
    expect(getByText("Entity Types")).toBeInTheDocument();
    expect(getByText("Instances")).toBeInTheDocument();
    expect(getByText("Last Processed")).toBeInTheDocument();

    expect(getByLabelText("add-entity")).toBeDisabled();

    // test add, save, revert icons display correct tooltip when disabled
    fireEvent.mouseOver(getByText("Add"));
    await wait(() => expect(getByText(ModelingTooltips.addNewEntity + " " + ModelingTooltips.noWriteAccess)).toBeInTheDocument());
    fireEvent.mouseOver(getByText("Save All"));
    await wait(() => expect(getByText(ModelingTooltips.saveAll + " " + ModelingTooltips.noWriteAccess)).toBeInTheDocument());
    fireEvent.mouseOver(getByText("Revert All"));
    await wait(() => expect(getByText(ModelingTooltips.revertAll + " " + ModelingTooltips.noWriteAccess)).toBeInTheDocument());

    expect(getByLabelText("save-all")).toBeDisabled();
    expect(queryByLabelText("entity-modified-alert")).toBeNull();
  });

  test("Modeling: can not see data if user does not have entity model reader role", async () => {
    mockPrimaryEntityType.mockResolvedValueOnce({status: 200, data: getEntityTypes});

    const {queryByText, queryByLabelText} = render(
      <AuthoritiesContext.Provider value={mockHCUserRolesService}>
        <ModelingContext.Provider value={notModified}>
          <Router>
            <Modeling/>
          </Router>
        </ModelingContext.Provider>
      </AuthoritiesContext.Provider>
    );

    await wait(() => expect(mockPrimaryEntityType).toHaveBeenCalledTimes(0));
    expect(queryByText("Entity Types")).toBeNull();
    expect(queryByText("Instances")).toBeNull();
    expect(queryByText("Last Processed")).toBeNull();

    expect(queryByLabelText("add-entity")).toBeNull();
    expect(queryByLabelText("save-all")).toBeNull();
    expect(queryByLabelText("entity-modified-alert")).toBeNull();
  });
});

describe("getViewSettings", () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    jest.restoreAllMocks();
  });

  const sessionStorageMock = (() => {
    let store = {};

    return {
      getItem(key) {
        return store[key] || null;
      },
      setItem(key, value) {
        store[key] = value.toString();
      },
      removeItem(key) {
        delete store[key];
      },
      clear() {
        store = {};
      }
    };
  })();

  Object.defineProperty(window, "sessionStorage", {
    value: sessionStorageMock
  });

  it("should get entity expanded rows from session storage", () => {
    const getItemSpy = jest.spyOn(window.sessionStorage, "getItem");
    window.sessionStorage.setItem("dataHubViewSettings", JSON.stringify({model: {entityExpandedRows: ["Customer,"]}}));
    const actualValue = getViewSettings();
    expect(actualValue).toEqual({model: {entityExpandedRows: ["Customer,"]}});
    expect(getItemSpy).toBeCalledWith("dataHubViewSettings");
  });

  it("should get property expanded rows from session storage", () => {
    const getItemSpy = jest.spyOn(window.sessionStorage, "getItem");
    window.sessionStorage.setItem("dataHubViewSettings", JSON.stringify({model: {propertyExpandedRows: ["shipping,31"]}}));
    const actualValue = getViewSettings();
    expect(actualValue).toEqual({model: {propertyExpandedRows: ["shipping,31"]}});
    expect(getItemSpy).toBeCalledWith("dataHubViewSettings");
  });

  it("should get entity and property expanded rows from session storage", () => {
    const getItemSpy = jest.spyOn(window.sessionStorage, "getItem");
    window.sessionStorage.setItem("dataHubViewSettings", JSON.stringify({model: {entityExpandedRows: ["Customer,"], propertyExpandedRows: ["shipping,31"]}}));
    const actualValue = getViewSettings();
    expect(actualValue).toEqual({model: {entityExpandedRows: ["Customer,"], propertyExpandedRows: ["shipping,31"]}});
    expect(getItemSpy).toBeCalledWith("dataHubViewSettings");
  });

  it("should get empty object if no info in session storage", () => {
    const getItemSpy = jest.spyOn(window.sessionStorage, "getItem");
    const actualValue = getViewSettings();
    expect(actualValue).toEqual({});
    expect(window.sessionStorage.getItem).toBeCalledWith("dataHubViewSettings");
    expect(getItemSpy).toBeCalledWith("dataHubViewSettings");
  });
});


